package app.park.com.vr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
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
import app.park.com.bluetooth.Protocol;
import app.park.com.task.MissionManager;

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

    public static final boolean USE_ASSET_PATH = true;
    private static final String DEFAULT_VIDEO_NAME = "car1.mp4";
    private static final String SECOND_VIDEO_NAME = "car2.mp4";
    private static final String THIRD_VIDEO_NAME = "car3.mp4";
    private static MissionManager mMissioManager;

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

    //speed value sequence in array from bluetooth
    private static final int  PROTOCOL_MSG_SPEED = 1;

    /**
     * Arbitrary constants and variable to track load status. In this example, this variable should
     * only be accessed on the UI thread. In a real app, this variable would be code that performs
     * some UI actions when the video is fully loaded.
     */
    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;

    //speed factor
    public static final double DEFAULT_SPEED = 0.0;
    public static final double MAX_SPEED = 1.5;
    private static double speed;
    private static double preSpeed;
    private static long currTime = 0;
    private static long prevTime = 0;


    //speed factor to control speed (get it through bluetooth panel)
    private double mSpeed;


    //time
    public static final int TIME_THRESHOLD_SECOND = 1000;
    public static final long REVERSE_TIME = 15000; // 15 seconds
    public static final int TIME_GAP = 14;

    //game score
    public static final int GAMEOVER_SCORE = 70;
    private static int score = 100;

    //video
    private int loadVideoStatus;
    private Uri fileUri;    // Tracks the file to be loaded across the lifetime of this app.
    private Options videoOptions = new Options();  // Configuration information for the video.
    private VideoLoaderTask backgroundVideoLoaderTask;
    private VrVideoView mVrVideoView;  // The video view and its custom UI elements.

    //status boolean
    private static boolean isPlaying = false;
    private static boolean isGameOver = false;
    private boolean isPaused = false;

    /**
     * By default, the video will start playing as soon as it is loaded. This can be changed by using
     * {@link VrVideoView#pauseVideo()} after loading the video.
     */
    private BluetoothService mBluetoothService = null;
    private BluetoothHandler mHandler = null;
    private BluetoothHandler.ActivityCb mActivityCb = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        // Bind input and output objects for the view.
        mVrVideoView = (VrVideoView) findViewById(R.id.video_view);
        mVrVideoView.setEventListener(new ActivityEventListener());
        //화면 자동 꺼짐 방지
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        //Bluetooth Service
        mBluetoothService = BluetoothService.getInstance();
        handleIntent(getIntent());
    }

    /**
     * Called when the Activity is already running and it's given a new intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) {
            Log.i(TAG, this.hashCode() + ".onNewIntent()" + " intent = " + intent.toString());
        }
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
        if (DBG) {
            Log.i(TAG, "intent = " + intent.toString());
        }
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
            Log.i(TAG, "Intent has not ACTION_VIEW. Using the default video.");
            fileUri = null;
            videoOptions.inputFormat = Options.FORMAT_DEFAULT;
            videoOptions.inputType = Options.TYPE_MONO;
        }

        // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
        // take 100s of milliseconds.
        if (backgroundVideoLoaderTask != null) {
            // Cancel any task from a previous intent sent to this activity.
            backgroundVideoLoaderTask.cancel(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
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
                                Toast.makeText(getApplicationContext(), R.string.info_msg_bt_disconnected, Toast.LENGTH_SHORT).show();
                                finish();
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        if(msg != null) {
                            String[] arr = msg.split(Protocol.SEPARATOR);

                            //명령어 실행 from bluetooth
                            executeCommand(arr);
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

        //영상 시작 guide message
        Toast.makeText(getApplicationContext(), R.string.info_msg_select_video, Toast.LENGTH_SHORT).show();
    }

    //set selected video path
    private Uri getVideoPATH(String number) {
        Uri path = null;
        switch (number) {
            case Protocol.PLAY_VIDEO_NUMBER1:
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                if (USE_ASSET_PATH == false) {
                    path = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + DEFAULT_VIDEO_NAME);
                }
                break;
            case Protocol.PLAY_VIDEO_NUMBER2:
                path = Uri.parse(SECOND_VIDEO_NAME);
                if (USE_ASSET_PATH == false) {
                    path = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + SECOND_VIDEO_NAME);
                }
                break;
            case Protocol.PLAY_VIDEO_NUMBER3:
                path = Uri.parse(THIRD_VIDEO_NAME);
                if (USE_ASSET_PATH == false) {
                    path = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + THIRD_VIDEO_NAME);
                }
                break;
            default:
                Log.w(TAG, "Video number has error. So we should use default value.");
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                break;
        }

        return path;
    }

    private void executeCommand(String[] arr) {
        // msg protocol : cmd////velocity////handle[0,1,2]////signalLight[0,1,2]////accel[0,1]////break[0,1]
        // play||||1,2,3 -> 1,2,3번 동영상 재생
        // stop||||1,2,3 -> 현재 동영상 멈춤
        String cmd = arr[Protocol.INDEX_CMD];
        switch (cmd) {
            case Protocol.CMD_PLAY:
                MissionManager.setVideoNumber(arr[Protocol.INDEX_PLAY_NUM]);
                fileUri = getVideoPATH(arr[Protocol.INDEX_PLAY_NUM]);
                backgroundVideoLoaderTask = new VideoLoaderTask();
                backgroundVideoLoaderTask.execute(Pair.create(fileUri, videoOptions));
                break;

            case Protocol.CMD_PAUSE:
                pauseVideo();
                break;

            case Protocol.CMD_STOP:
                pauseVideo();
                mVrVideoView.seekTo(0);
                currTime = 0;
                finish();
                break;

            case Protocol.CMD_GAMERUN:
                // 비디오가 정상적으로 로드 되었을때 부터 게임 처리
                if (getLoadVideoStatus() == LOAD_VIDEO_STATUS_SUCCESS) {
                    //Speed
                    if (arr[PROTOCOL_MSG_SPEED] != null) {
                        speed = Double.parseDouble((arr[PROTOCOL_MSG_SPEED]));
                        //speed가 기존과 다른 경우만 전송
                        if (preSpeed != speed) {
                            preSpeed = speed;
                            setSpeed(speed);
                        }
                    }
                    if (isPlaying == false) { // 비디오 Pasue 상태
                        if (speed > 0 && (isGameOver==false)) { // speed 가 0보다 크면 엑셀을 누른 상태
                            playVideo();
                        }
                    } else { // 비디오 Play 상태
                        if (speed == 0) { // speed가 0이면 동작 않하는 상태, 게임 체크할 필요도 없
                            pauseVideo();
                        } else {
                            //TODO
                            //1. Senario에 arr을 던져 현재 penalty값을 체크 pass/fail (0이면 pass, 0 미만이면 fail)
                            //2. fail인경우, Score 업데이트
                            //3. 70점이하인지 체크
                            //4. 70점 이하면 컨트롤러에 Ack 전송(컨트롤 메시지 보내지 못하도록)
                            //5. 비디오 포즈
                            //6. 게임 종료 팝업 & 다시할지 여부 체크
                            //score 계산을 위해서 arr. 던져주고 감점되는 값을 받아옴
                            int penalty = MissionManager.validate(arr, mVrVideoView.getCurrentPosition());
                            if(penalty != 0) {
                                setScore(penalty);
                                if (getScore() < GAMEOVER_SCORE) {
                                    pauseVideo();
                                    isGameOver = true;
                                    Toast.makeText(getApplicationContext(), " 70점 이하 fail!!", Toast.LENGTH_LONG).show();
                                    mBluetoothService.sendMessage(Protocol.CMD_RESUME);
                                    AlertDialog gameOverDialog = crateGameOverDialog();
                                    gameOverDialog.show();
                                } else {
                                    mBluetoothService.sendMessage(Protocol.CMD_REWIND);
                                    Toast.makeText(getApplicationContext(), " fail!! 5초전으로 돌림", Toast.LENGTH_SHORT).show();
                                    long videoTime = mVrVideoView.getCurrentPosition();
                                    if (videoTime > REVERSE_TIME) {
                                        videoTime -= REVERSE_TIME;
                                    } else {
                                        videoTime = 0;
                                    }
                                    MissionManager.reInit();
                                    mVrVideoView.seekTo(videoTime);
                                    currTime = videoTime;
                                    pauseVideo();
                                }
                            }
                        }
                    }
                }
                break;
        }
    }

    protected void pauseVideo() {
        mVrVideoView.pauseVideo();
        isPlaying = false;
        isPaused = true;
    }

    protected void playVideo() {
        mVrVideoView.playVideo();
        isPlaying = true;
        isPaused = false;
    }

    protected void resetVideo() {
        mVrVideoView.seekTo(0);
        currTime = 0;
    }

    //set speed factor
    private void setSpeed(double speed) {
        //0.5가 기본 속도 (0.5배 느림)
        //1이 정상 속도
        //1.5가 빠른 속도 (1.5배 빠름)
        if (mSpeed >= 0) {
            mSpeed = speed;
        }
        if (mSpeed > MAX_SPEED) { // MAX Speed를 초과하지는 못하도록 설정
            mSpeed = MAX_SPEED;
        }
        if (mSpeed < DEFAULT_SPEED) { // Default Speed 보다 작으면 무조건 0으로 설정
            mSpeed = 0;
        }
    }

    //calculate milliseconds for adding on current duration of video with speed factor
    public double getAddSpeed() {
        double addSpeed;
        //0.5배 = 70밀리세컨드
        //1배 = 140 밀리세컨드(seekto를 호출하지 않음)
        //1.5배 = 210 밀리세컨드
        addSpeed = mSpeed*10*TIME_GAP;
        return addSpeed;
    }

    //set score
    private void setScore(int penalty) {
        score += penalty;
    }

    //get current score
    public int getScore() {
        return score;
    }

    //reset score
    public void resetScore() {
        score = 100;
    }

    @Override
    public void onBackPressed() {
            AlertDialog backPressedDialog = createBackPressedDialog();
            backPressedDialog.show();
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
        currTime = progressTime;

        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            pauseVideo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVrVideoView != null) {
            mVrVideoView.resumeRendering(); // Resume the 3D rendering.
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        if (mVrVideoView != null) {
            mVrVideoView.shutdown(); // Destroy the widget and free memory.
        }
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
            mVrVideoView.playVideo();
            isPlaying = true;
        } else {
            mVrVideoView.pauseVideo();
            isPlaying = false;
        }
        isPaused = !isPaused;
    }

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }

    public void reset() {
        isGameOver = false;
        resetVideo();
        resetScore();
        MissionManager.reInit();
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
            //mVrVideoView.pauseVideo();
            pauseVideo();
        }

        /**
         * Called by video widget on the UI thread on any asynchronous error.
         */
        @Override
        public void onLoadError(String errorMessage) {
            // An error here is normally due to being unable to decode the video format.
            loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
            Toast.makeText(VrVideoActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_SHORT).show();
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
                prevTime = currTime;
                long addSpeed = (long) getAddSpeed();
                long videoPosition = mVrVideoView.getCurrentPosition();
                if (speed != 140) {
                    videoPosition = (long)prevTime + addSpeed;
                    mVrVideoView.seekTo(videoPosition);
                }
                currTime = videoPosition;
            }
        }

        /**
         * Make the video play in a loop. This method could also be used to move to the next video in
         * a playlist.
         */
        @Override
        public void onCompletion() {
            mVrVideoView.seekTo(0);
            currTime = 0;
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
            Log.d(TAG, "onPreExecute!!!");
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {
            Log.d(TAG, "doInBackground!!!");
            Boolean result = false;

            if (fileInformation == null || fileInformation.length < 1
                    || fileInformation[0] == null || fileInformation[0].first == null) {
                // No intent was specified, so we default to playing the local stereo-over-under video.
                Options options = new Options();
                options.inputFormat = Options.FORMAT_DEFAULT;
                options.inputType = Options.TYPE_MONO;
                videoOptions = options;
                result = true;
            } else {
                videoUri = fileInformation[0].first;
                videoOptions = fileInformation[0].second;
            }
            Log.d(TAG, "doInBackground out !!");
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            Log.d(TAG, "onProgressUpdate!!!");
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            try {
                Log.d(TAG, "onPostExecute!!!");
                if (aBoolean) {
                    mVrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    mVrVideoView.setInfoButtonEnabled(false);
                    mVrVideoView.setFullscreenButtonEnabled(false);
                    String videoName = DEFAULT_VIDEO_NAME;
                    if (DBG) { Log.d(TAG, "### loadVideoFromAsset()"); }
                    mVrVideoView.loadVideoFromAsset(videoName, videoOptions);
                } else {
                    if (DBG) { Log.d(TAG, "### Video path : " + videoUri.toString()); }
                    mVrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    mVrVideoView.setInfoButtonEnabled(false);
                    mVrVideoView.setFullscreenButtonEnabled(false);
                    if (USE_ASSET_PATH) {
                        mVrVideoView.loadVideoFromAsset(videoUri.toString(), videoOptions);
                    } else {
                        mVrVideoView.loadVideo(videoUri, videoOptions);
                    }
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

    //pop up for gameover
    private AlertDialog crateGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("GameOver");
        builder.setMessage("다시 시작 하시겠습니까?\nNo를 선택하시면 종료됩니다.");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                reset();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                mBluetoothService.sendMessage(Protocol.CMD_STOP);
                reset();
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        return dialog;
    }

    //pop up for back key
    private AlertDialog createBackPressedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Quit");
        builder.setMessage("Are you sure you want to quit??");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                mBluetoothService.sendMessage(Protocol.CMD_STOP);
                togglePause();
                mVrVideoView.seekTo(0);
                currTime = 0;
                finish();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
            }
        });

        AlertDialog dialog = builder.create();
        return dialog;
    }
}
