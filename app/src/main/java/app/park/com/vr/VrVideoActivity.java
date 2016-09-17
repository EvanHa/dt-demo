package app.park.com.vr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.google.vr.sdk.widgets.video.VrVideoView.Options;

import java.io.File;
import java.io.IOException;

import app.park.com.R;
import app.park.com.bluetooth.BluetoothHandler;
import app.park.com.bluetooth.BluetoothService;
import app.park.com.bluetooth.Constants;

/**
 * A test activity that renders a 360 video using {@link VrVideoView}.
 * It loads the video in the assets by default. User can use it to load any video files using the
 * command:
 * adb shell am start -a android.intent.action.VIEW \
 * -n com.google.vr.sdk.samples.simplevideowidget/.SimpleVrVideoActivity \
 * -d /sdcard/FILENAME.MP4
 * <p/>
 * To load HLS urls add "--ei inputFormat 2" to pass in an integer extra which will set
 * VrVideoView.Options.inputFormat. e.g.
 * adb shell am start -a android.intent.action.VIEW \
 * -n com.google.vr.sdk.samples.simplevideowidget/.SimpleVrVideoActivity \
 * -d "https://EXAMPLE.COM/FILENAME.M3U8" \
 * --ei inputFormat 2
 * <p/>
 * To specify that the video is of type stereo over under (has images for left and right eyes),
 * add "--ei inputType 2" to pass in an integer extra which will set VrVideoView.Options.inputType.
 * This can be combined with other extras.
 * e.g.
 * adb shell am start -a android.intent.action.VIEW \
 * -n com.google.vr.sdk.samples.simplevideowidget/.SimpleVrVideoActivity \
 * -d "https://EXAMPLE.COM/FILENAME.MP4" \
 * --ei inputType 2
 */
public class VrVideoActivity extends Activity {
    public static final String TAG = VrVideoActivity.class.getSimpleName();
    public static final boolean DBG = true;

    /**
     * Preserve the video's state when rotating the phone.
     */
    private static final String STATE_IS_PAUSED = "isPaused";
    private static final String STATE_PROGRESS_TIME = "progressTime";
    /**
     * The video duration doesn't need to be preserved, but it is saved in this example. This allows
     * the seekBar to be configured during {@link #onRestoreInstanceState(Bundle)} rather than waiting
     * for the video to be reloaded and analyzed. This avoid UI jank.
     */
    private static final String STATE_VIDEO_DURATION = "videoDuration";

    /**
     * Arbitrary constants and variable to track load status. In this example, this variable should
     * only be accessed on the UI thread. In a real app, this variable would be code that performs
     * some UI actions when the video is fully loaded.
     */
    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;
    public static final int TIME_THRESHOLD_SECOND = 1000;
    public static final int TIME_THRESHOLD_FRAME_UPDATE = 500;
    public static final double DEFAULT_SPEED = 0.5;
    public static final double MAX_SPEED = 1.5;

    private int loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;
    private Uri fileUri;    // Tracks the file to be loaded across the lifetime of this app.
    private Options videoOptions = new Options();  // Configuration information for the video.
    private VideoLoaderTask backgroundVideoLoaderTask;
    private VrVideoView mVrVideoView;  // The video view and its custom UI elements.
    private SeekBar seekBar; //Seeking UI & progress indicator. The seekBar's progress value represents milliseconds in the video.
    private TextView statusText;

    //create speed factor to control speed (get it through bluetooth panel) by sw
    private double mSpeed;
    private int score;           //game score
    private int turn;            //turn direction: 1 right, 2 left

    //selection of video
    public static int SELECTED_VIDEO = 1;

    /**
     * By default, the video will start playing as soon as it is loaded. This can be changed by using
     * {@link VrVideoView#pauseVideo()} after loading the video.
     */
    private boolean isPaused = false;

    private BluetoothService mBluetoothService = null;
    private BluetoothHandler mHandler = null;
    private BluetoothHandler.ActivityCb mActivityCb = null;

    // For Debug
    private Button mSpeedUpTest;
    private Button mSpeedDownTest;
    private double testSpeed = 0;

    /*
    private LinearLayout myView;
    private TextView mText;
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBarListener());
        statusText = (TextView) findViewById(R.id.status_text);

        // Bind input and output objects for the view.
        mVrVideoView = (VrVideoView) findViewById(R.id.video_view);
        mVrVideoView.setEventListener(new ActivityEventListener());
/*
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myView = (LinearLayout) inflater.inflate(R.layout.activity_game_score, null);
        mText = (TextView) findViewById(R.id.test_txt);
*/

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        mBluetoothService = BluetoothService.getInstance();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 자동 꺼짐 방지

        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(getIntent());

        mSpeedUpTest = (Button) findViewById(R.id.speed_up_test);
        mSpeedUpTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testSpeed += 0.1;
                String string = String.valueOf(testSpeed);
                if (DBG) { Log.d(TAG, "Up button Click" + " testSpeed = " + string); }
                if (testSpeed > MAX_SPEED) {
                    testSpeed = MAX_SPEED;
                }
                setSpeed(testSpeed);
                updateStatusText();
            }
        });

        mSpeedDownTest = (Button) findViewById(R.id.speed_down_test);
        mSpeedDownTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testSpeed -= 0.1;
                String string = String.valueOf(testSpeed);
                if (DBG) { Log.d(TAG, "Down button Click" + " testSpeed = " + string); }
                if (testSpeed < 0) {
                    testSpeed = 0;
                }
                setSpeed(testSpeed);
                updateStatusText();
            }
        });
    }

    /**
     * Called when the Activity is already running and it's given a new intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, this.hashCode() + ".onNewIntent()");
        // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
        // future invocations.
        setIntent(intent);
        // Load the new image.
        handleIntent(intent);
    }

    /**
     * Load custom videos based on the Intent or load the default video. See the Javadoc for this
     * class for information on generating a custom intent via adb.
     */
    private void handleIntent(Intent intent) {
        // Determine if the Intent contains a file to load.
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.i(TAG, "ACTION_VIEW Intent received");

            fileUri = intent.getData();
            if (fileUri == null) {
                Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
            } else {
                Log.i(TAG, "Using file " + fileUri.toString());
            }

            videoOptions.inputFormat = intent.getIntExtra("inputFormat", Options.FORMAT_DEFAULT);
            videoOptions.inputType = intent.getIntExtra("inputType", Options.TYPE_MONO);
        } else {
            Log.i(TAG, "Intent is not ACTION_VIEW. Using the default video.");
            fileUri = null;
        }

        // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
        // take 100s of milliseconds.
        if (backgroundVideoLoaderTask != null) {
            // Cancel any task from a previous intent sent to this activity.
            backgroundVideoLoaderTask.cancel(true);
        }
        backgroundVideoLoaderTask = new VideoLoaderTask();
        backgroundVideoLoaderTask.execute(Pair.create(fileUri, videoOptions));
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("TAG","********Status-Start********");

        mHandler = new BluetoothHandler();
        mActivityCb = new BluetoothHandler.ActivityCb() {
            @Override
            public void sendCbMessage(int msgType, String msg) {
                Log.d(TAG, "msg = " + msg);
                switch (msgType) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (msg) {
                            case Constants.BLUETOOTH_CONNECTED:
                                break;
                            case Constants.BLUETOOTH_CONNECTING:
                                break;
                            case Constants.BLUETOOTH_NONE:
                                Toast.makeText(getApplicationContext(), "BT DISCONNECT!!!", Toast.LENGTH_SHORT).show();
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        String[] arr = msg.split("////");
                        Log.i("TAG", "*******" + msg);
                        Log.i("TAG", "*******" + arr.length);
                        Log.i("TAG", "*******" + arr[0]);

                        //명령어 실행 from bluetooth
                        executeCommand(arr);

                        break;
                    case Constants.MESSAGE_WRITE:

                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        break;
                    case Constants.MESSAGE_TOAST:
                        break;
                    default:
                        break;
                }
            }
        };
        mHandler.setActivityCb(mActivityCb);
        mBluetoothService.setActivityHandler(mHandler);
    }

    private void executeCommand (String[] arr){
        // play||||1,2,3 -> 1,2,3번 동영상 재생
        // stop||||1,2,3 -> 현재 동영상 멈춤
        // cmd|||||40||||90 -> 속도 40, 점수 90
        switch (arr[0]){
            case "play":
                int video = Integer.parseInt(arr[1]);
                setVideo(video);
                playVideo();
                break;

            case "stop":
                stopVideo();
                break;

            case "cmd":
                if(DBG){Log.i(TAG, "check - executeCommand");}
                int score = Integer.parseInt(arr[2]);
                setScore(score);

                double speed = Double.parseDouble((arr[1]));
                setSpeed(speed);

                break;
        }
    }

    protected void setVideo(int video){
        SELECTED_VIDEO = video;
    }

    protected String getVideo(){
        String videoName = null;
        switch (SELECTED_VIDEO){
            case 1:
                videoName = "car.mp4";
                break;

            case 2:
                videoName = "car.mp4";
                break;

            case 3:
                videoName = "car.mp4";
                break;

        }

        return videoName;
    }

    protected void stopVideo(){
        mVrVideoView.pauseVideo();
    }

    protected void playVideo(){
        mVrVideoView.playVideo();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, mVrVideoView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, mVrVideoView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        mVrVideoView.seekTo(progressTime);
        seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
        seekBar.setProgress((int) progressTime);

        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            mVrVideoView.pauseVideo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG) { Log.i("TAG","********Status-Resume********"); }
        if (mVrVideoView != null) {
            mVrVideoView.resumeRendering(); // Resume the 3D rendering.
        }
        updateStatusText(); // Update the text to account for the paused video in onPause().
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) { Log.i("TAG","********Status-Pause********"); }
        // Prevent the view from rendering continuously when in the background.
        if (mVrVideoView != null) {
            mVrVideoView.pauseRendering();
        }
        // If the video is playing when onPause() is called, the default behavior will be to pause
        // the video and keep it paused when onResume() is called.
        isPaused = true;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DBG) { Log.i("TAG","********Status-Destroy********"); }
        if (mVrVideoView != null) {
            mVrVideoView.shutdown(); // Destroy the widget and free memory.
        }
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
           if(DBG){Log.i("TAG","********Status-playvideo********");}
            mVrVideoView.playVideo();
//            mText.setText("Play");
        } else {
            if(DBG){Log.i("TAG","********Status-pausevideo********");}
            mVrVideoView.pauseVideo();
//            mText.setText("Pause");
        }
        isPaused = !isPaused;
        updateStatusText();
    }

    private void updateStatusText() {
        StringBuilder status = new StringBuilder();
        status.append(isPaused ? "Paused: " : "Playing: ");
        status.append(String.format("%.2f", mVrVideoView.getCurrentPosition() / 1000f));
        status.append(" / ");
        status.append(mVrVideoView.getDuration() / 1000f);
        status.append(" seconds.");
        status.append("Speed(ms): ");
        status.append(getSpeedToMilliSecond());
        statusText.setText(status.toString());
    }

    protected void handleCommand(String[] arr){

    }

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }

    //set speed factor(multiplication of 1000 to make factor as millisecond)
    private void setSpeed(double speed){
//        if (speed > 0) { // 입력값이 양수
        if (mSpeed >= 0) { // 기존값이 없거나, 있었다고 하면 Default + (speed - 0.1)
            mSpeed = (DEFAULT_SPEED + (speed - 0.1));
        }
        if (mSpeed > MAX_SPEED) { // MAX Speed를 초과하지는 못하도록 설정
            mSpeed = MAX_SPEED;
        }
        if (mSpeed < DEFAULT_SPEED) { // Default Speed 보다 작으면 무조건 0으로 설정
            mSpeed = 0;
        }
//        } else if (speed < 0) { // 입력값이 음수
//            if (mSpeed > 0) { // 기존에 값이 뭔가라도 있다면, 입력받은 Speed값을 합쳐서 작아지게 함.
//                mSpeed += speed;
//            }
//            if (mSpeed < DEFAULT_SPEED) { // Default Speed 보다 작으면 무조건 0으로 설정
//                mSpeed = 0;
//            }
//        } else {    // 입력값이 0
//            mSpeed = 0;
//        }    }
    }

    public double getSpeedToMilliSecond() {
        return (mSpeed * TIME_THRESHOLD_SECOND);
    }

    public double getSpeedToSecond(){
        return mSpeed;
    }

    //set score
    private void setScore(int i){
        score = i;
    }

    //get current score
    public int getScore(){
        return score;
    }

    //set turn factor
    private void setTurn(int i){
        turn = i;
    }

    //get current turn factor
    public int getTurn(){
        return turn;
    }

    public void reset(){
        togglePause();
        mVrVideoView.seekTo(0);
        updateStatusText();
    }

    /**
     * When the user manipulates the seek bar, update the video position.
     */
    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mVrVideoView.seekTo(progress);
                updateStatusText();
            } // else this was from the ActivityEventHandler.onNewFrame()'s seekBar.setProgress update.
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /**
     * Listen to the important events from widget.
     */
    private class ActivityEventListener extends VrVideoEventListener {

        /**
         * Called by video widget on the UI thread when it's done loading the video.
         */
        @Override
        public void onLoadSuccess() {
            Log.i(TAG, "Sucessfully loaded video " + mVrVideoView.getDuration());
            loadVideoStatus = LOAD_VIDEO_STATUS_SUCCESS;
            seekBar.setMax((int) mVrVideoView.getDuration());
            updateStatusText();
        }

        /**
         * Called by video widget on the UI thread on any asynchronous error.
         */
        @Override
        public void onLoadError(String errorMessage) {
            // An error here is normally due to being unable to decode the video format.
            loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
            Toast.makeText(VrVideoActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error loading video: " + errorMessage);
        }

        @Override
        public void onClick() {
            togglePause();
        }

        /**
         * Update the UI every frame.
         */
        @Override
        //change onNewFrame for play speed control with speed factor from bluetooth
        public void onNewFrame() {
            if (!isPaused) {
                long speed = (long)getSpeedToMilliSecond();
                long videoPosition = mVrVideoView.getCurrentPosition();
                if (speed != 0) {
                    videoPosition += speed;
                    mVrVideoView.seekTo(videoPosition);
                }
                seekBar.setProgress((int)videoPosition);
                updateStatusText();
            }
        }

        /**
         * Make the video play in a loop. This method could also be used to move to the next video in
         * a playlist.
         */
        @Override
        public void onCompletion() {
            mVrVideoView.seekTo(0);
        }
    }

    /**
     * Helper class to manage threading.
     */
    class VideoLoaderTask extends AsyncTask<Pair<Uri, Options>, Void, Boolean> {

        protected Uri videoUri;
        protected Options videoOptions;

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute!!!");
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {
            Log.d(TAG,"doInBackground!!!");
            Boolean result = false;

                if (fileInformation == null || fileInformation.length < 1
                        || fileInformation[0] == null || fileInformation[0].first == null) {
                    // No intent was specified, so we default to playing the local stereo-over-under video.
                    Options options = new Options();
                    options.inputFormat = Options.FORMAT_DEFAULT;
                    options.inputType = Options.TYPE_MONO;
                    videoOptions = options;
//                    //bluetooth에서 select한 video를 가져온다
//                    if(DBG){ Log.i(TAG, "check - uri setting");}
//                    String videoName = getVideo();
//                    videoUri = Uri.parse(videoName);

                    //test 용 uri
                    videoUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + "car.mp4");


                    result = true;
                } else {
                    videoOptions = fileInformation[0].second;
                    videoUri = fileInformation[0].first;
                }
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Log.d(TAG,"onProgressUpdate!!!");
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            try {
                Log.d(TAG,"onPostExecute!!!");
                if (aBoolean) {
                    mVrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_MONO);
                    mVrVideoView.setStereoModeButtonEnabled(false);

                    //bluetooth에서 select한 video를 가져온다
//                    String videoName = getVideo();
//                    if(DBG){Log.i(TAG, "check - onPostexecute");}
//                    mVrVideoView.loadVideoFromAsset(videoName, videoOptions);

                    //mVrVideoView.loadVideoFromAsset(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + "car.mp4", videoOptions);
                    mVrVideoView.loadVideo(videoUri, videoOptions);
                    Log.d(TAG, "### loadVideoFromAsset()");
                } else {
                    mVrVideoView.loadVideo(videoUri, videoOptions);
                    Log.d(TAG, "### loadVideo()");
                }
/*
                WindowManager.LayoutParams myParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                WindowManager myWindow = (WindowManager) getSystemService(WINDOW_SERVICE);
                myWindow.addView(myView, myParams);
                */
            } catch (IOException e) {
                e.printStackTrace();
                // An error here is normally due to being unable to locate the file.
                loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
                // Since this is a background thread, we need to switch to the main thread to show a toast.
                mVrVideoView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(VrVideoActivity.this, "Error opening file. ", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Could not open video: " + e);
            }
            super.onPostExecute(aBoolean);
        }
    }
}
