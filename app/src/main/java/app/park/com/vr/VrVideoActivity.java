package app.park.com.vr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private int loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }

    /**
     * Tracks the file to be loaded across the lifetime of this app.
     **/
    private Uri fileUri;

    /**
     * Configuration information for the video.
     **/
    private Options videoOptions = new Options();
    private VideoLoaderTask backgroundVideoLoaderTask;

    /**
     * The video view and its custom UI elements.
     */
    protected VrVideoView videoWidgetView;

    /**
     * Seeking UI & progress indicator. The seekBar's progress value represents milliseconds in the
     * video.
     */
    private SeekBar seekBar;
    private TextView statusText;

    //create speed factor to control speed (get it through bluetooth panel) by sw
    private double mSpeed;
    private int score;           //game score
    private int turn;            //turn direction: 1 right, 2 left

    //broadcast receiver message
    private final String BROADCAST_MESSAGE = "com.broadcastreceiver.VRVIDEOACTIVITY";
    //broadcast receiver
    BroadcastReceiver brRev;

    /**
     * By default, the video will start playing as soon as it is loaded. This can be changed by using
     * {@link VrVideoView#pauseVideo()} after loading the video.
     */
    private boolean isPaused = false;

    private BluetoothService mBluetoothService = null;
    private BluetoothHandler mHandler = null;
    private BluetoothHandler.ActivityCb mActivityCb = null;

    private Button mSpeedTest;
    private double prevTime;
    private double currTime;
    private long prevVideoTime;
    private long currVideoTime;
    private int mPrevCount;
    private int mCurrCount;
    private boolean mUpdateFlag = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBarListener());
        statusText = (TextView) findViewById(R.id.status_text);

        mSpeedTest = (Button) findViewById(R.id.speed_test);
        mSpeedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Click");
            }
        });

        mPrevCount = 0;
        mCurrCount = 0;

        //brRev = new BroadcastReceiver();

//        //by sw
//        btn_accel = (Button) findViewById(R.id.btn_accel);
//        btn_accel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                speed = ((double)getSpeed()/1000);
//                if(speed < 1.5){
//                    speed = speed + 0.1;
//                }else{
//
//                }
//                Log.i("TAG","accel: " + speed);
//                setSpeed(speed);
//            }
//        });
//
//        btn_break = (Button) findViewById(R.id.btn_break);
//        btn_break.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                speed = ((double)getSpeed()/1000);
//                if(speed >= 0.5){
//                    speed = speed - 0.5;
//                }else if(speed < 0.5){
//                    speed = 0;
//                }
//                Log.i("TAG","break: " + speed);
//                setSpeed(speed);
//            }
//        });
//
//        btn_reset = (Button) findViewById(R.id.btn_reset);
//        btn_reset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                reset();
//            }
//        });


        // Bind input and output objects for the view.
        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setEventListener(new ActivityEventListener());

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(getIntent());
        mBluetoothService = BluetoothService.getInstance();
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        videoWidgetView.seekTo(progressTime);
        seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
        seekBar.setProgress((int) progressTime);

        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            videoWidgetView.pauseVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Prevent the view from rendering continuously when in the background.
        videoWidgetView.pauseRendering();
        // If the video is playing when onPause() is called, the default behavior will be to pause
        // the video and keep it paused when onResume() is called.
        isPaused = true;
        Log.i("TAG","********Status-Pause********");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register Receiver

        registerReceiver();
        // Resume the 3D rendering.
        videoWidgetView.resumeRendering();
        // Update the text to account for the paused video in onPause().
        updateStatusText();

        Log.i("TAG","********Status-Resume********");
    }

    @Override
    protected void onDestroy() {
        //unregister Receiver
        unregisterReceiver();
        // Destroy the widget and free memory.
        videoWidgetView.shutdown();
        super.onDestroy();
        Log.i("TAG","********Status-Destroy********");
    }

    private void togglePause() {
        if (isPaused) {
            videoWidgetView.playVideo();
            Log.i("TAG","********Status-playvideo********");
        } else {
            videoWidgetView.pauseVideo();
            Log.i("TAG","********Status-pausevideo********");
        }
        isPaused = !isPaused;
        updateStatusText();
    }

    private void updateStatusText() {
        StringBuilder status = new StringBuilder();
        status.append(isPaused ? "Paused: " : "Playing: ");
        status.append(String.format("%.2f", videoWidgetView.getCurrentPosition() / 1000f));
        status.append(" / ");
        status.append(videoWidgetView.getDuration() / 1000f);
        status.append(" seconds.");
        statusText.setText(status.toString());
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

    protected void handleCommand(String[] arr){

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    //set speed factor(multiplication of 1000 to make factor as millisecond)
    private void setSpeed(double speed){
        if(speed != 0){
            mSpeed = 1000 * speed;
        }
    }

    //get current speed
    public double getSpeed(){
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
        videoWidgetView.seekTo(0);
        updateStatusText();
    }

    private void registerReceiver(){
        Log.i("TAG","********brRev********" + brRev);

        if(brRev != null) return;

        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(BROADCAST_MESSAGE);

        this.brRev = new BroadcastReceiver();

        this.registerReceiver(this.brRev, theFilter);

        Log.i("TAG","********register receiver********");
    }

    private void unregisterReceiver() {
        if(brRev != null)
            this.unregisterReceiver(brRev);

        Log.i("TAG","********unregister receiver********");
    }

    //braodcast receiver to get command from bluetooth
    private class BroadcastReceiver extends android.content.BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String[] cmd = bundle.getString("cmd").split("||||");
            if(cmd.length == 2){
            }
        }
    }

    /**
     * When the user manipulates the seek bar, update the video position.
     */
    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                videoWidgetView.seekTo(progress);
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
            Log.i(TAG, "Sucessfully loaded video " + videoWidgetView.getDuration());
            loadVideoStatus = LOAD_VIDEO_STATUS_SUCCESS;
            seekBar.setMax((int) videoWidgetView.getDuration());
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
            currTime = videoWidgetView.getCurrentPosition();
            Log.d(TAG, " Time gap = " + (currTime-prevTime));
            if (!isPaused) {
                //videoWidgetView.pauseVideo();
                double speed = getSpeed();
                long videoPosition = videoWidgetView.getCurrentPosition();
                seekBar.setProgress((int)videoPosition);
                mCurrCount = (int)videoPosition/500;
                if (((mCurrCount-mPrevCount)>0)) {
                    videoWidgetView.pauseVideo();
                    videoWidgetView.seekTo(videoPosition+500);
                    videoWidgetView.playVideo();
                }
                mPrevCount = mCurrCount;
                //Log.i(TAG, " vP " + videoPosition);
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
            videoWidgetView.seekTo(0);
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
                    //options.inputType = Options.TYPE_STEREO_OVER_UNDER;
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
                    videoWidgetView.setDisplayMode(2);
                    videoWidgetView.loadVideoFromAsset("car.mp4", videoOptions);
                } else {
                    videoWidgetView.loadVideo(videoUri, videoOptions);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // An error here is normally due to being unable to locate the file.
                loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
                // Since this is a background thread, we need to switch to the main thread to show a toast.
                videoWidgetView.post(new Runnable() {
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
