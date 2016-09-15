package app.park.com.vr;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.common.VrWidgetView;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.google.vr.sdk.widgets.video.VrVideoView.Options;

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
    private double prevTime;
    private double currTime;
    private long prevVideoTime;
    private long currVideoTime;
    private int mPrevCount;
    private int mCurrCount;
    private boolean mUpdateFlag = false;
    private double testSpeed = 0;

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

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(getIntent());
        mBluetoothService = BluetoothService.getInstance();

        mSpeedUpTest = (Button) findViewById(R.id.speed_up_test);
        mSpeedUpTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DBG) { Log.d(TAG, "Click"); }
                testSpeed += 0.1;
                setSpeed(testSpeed);
            }
        });

        mSpeedDownTest = (Button) findViewById(R.id.speed_down_test);
        mSpeedDownTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DBG) { Log.d(TAG, "Click"); }
                testSpeed -= 0.1;
                if (testSpeed < 0) {
                    testSpeed = 0;
                }
                setSpeed(testSpeed);
            }
        });
        mPrevCount = 0;
        mCurrCount = 0;
        prevVideoTime = 0;
        currVideoTime = 0;
        mPrevCount = 0;
        mPrevCount = 0;
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

                        // play||||1,2,3 -> 1,2,3번 동영상 재생
                        // stop||||1,2,3 -> 현재 동영상 멈춤
                        if(arr.length == 2) {
                            if(arr[0].equals("play")){
                                //play
                            }else if(arr[0].equals("stop")){
                                //stop
                            }
                            // cmd|||||40||||90 -> 속도 40, 점수 90
                        }else if(arr.length == 3){
                            // 해당 속도, 점수 업뎃 쳐줌
                            int score = Integer.parseInt(arr[2]);
                            setScore(score);

                            double speed = Double.parseDouble((arr[1]));
                            setSpeed(speed);

                            if(arr[0].equals("play")){
                                //onResume();
                            }else if(arr[0].equals("stop")){
                                //onPause();
                            }
                        }
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
            Log.i("TAG","********Status-playvideo********");
            mVrVideoView.playVideo();
        } else {
            Log.i("TAG","********Status-pausevideo********");
            mVrVideoView.pauseVideo();
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
        if(speed != 0) { // Speed값이 있으면
            if (mSpeed==0) { // 기존 Speed 값이 0이면 처음이라는 의미
                if (speed > 0) { // 처음부터 음수값이 들어올수도 있으니, 양수 일때만 처리
                    mSpeed = DEFAULT_SPEED; // 0.5부터 1초에 0.1씩이니깐, 기본이 0.5
                }
            } else {
                mSpeed += speed; // speed가 양수든 음수든 기존 값이랑 일단 합치고,
                if (mSpeed >= MAX_SPEED) { // Max를 넘었으면 Max로 다시 조정
                    mSpeed = MAX_SPEED;
                } else if (mSpeed < DEFAULT_SPEED) { // 기본값보다 작으면 0으로 조정
                    mSpeed = 0;
                }
            }
        } else { // Speed 값이 0이면 0으로
            mSpeed = 0;
        }
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
            prevTime = currTime;
            currTime = mVrVideoView.getCurrentPosition();
            Log.d(TAG, " Time gap = " + (currTime-prevTime));
            if (!isPaused) {
                //videoWidgetView.pauseVideo();
                long speed = (long)getSpeedToMilliSecond();
                long videoPosition = mVrVideoView.getCurrentPosition();
                mCurrCount = (int)videoPosition/TIME_THRESHOLD_FRAME_UPDATE;
                if (((mCurrCount-mPrevCount) > 0)) {
                    if (speed != 0) {
                        videoPosition += speed;
                        //mVrVideoView.pauseVideo();
                        mVrVideoView.seekTo(videoPosition);
                        //mVrVideoView.playVideo();
                    }
                }
                seekBar.setProgress((int)videoPosition);
                mPrevCount = mCurrCount;
                Log.d(TAG, " Time gap = " + (videoPosition-prevVideoTime));
                prevVideoTime = videoPosition;
                //videoWidgetView.playVideo();
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
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {

            Boolean result = false;

                if (fileInformation == null || fileInformation.length < 1
                        || fileInformation[0] == null || fileInformation[0].first == null) {
                    // No intent was specified, so we default to playing the local stereo-over-under video.
                    Options options = new Options();
                    options.inputFormat = Options.FORMAT_DEFAULT;
                    options.inputType = Options.TYPE_MONO;
                    videoOptions = options;
                    videoUri = Uri.parse("car.mp4");
                    result = true;
                } else {
                    videoOptions = fileInformation[0].second;
                    videoUri = fileInformation[0].first;
                }
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            try {
                if (aBoolean) {
                    mVrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_MONO);
                    mVrVideoView.setStereoModeButtonEnabled(false);
                    mVrVideoView.loadVideoFromAsset("car.mp4", videoOptions);
                } else {
                    mVrVideoView.loadVideo(videoUri, videoOptions);
                }
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
