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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
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
import app.park.com.task.TaskValidator;

import static app.park.com.R.layout.popup;

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
    private static final String DEFAULT_VIDEO_NAME = "car.mp4";

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

    // msg protocol : cmd////velocity////handle[0,1]////signalLight[0,1,2]////accel[0,1]////break[0,1]
    private static final int  PROTOCOL_MSG_CMD = 0;
    private static final int  PROTOCOL_MSG_PLAY_NUM = 1; // If command is play, next value is play video number
    private static final int  PROTOCOL_MSG_SPEED = 1;
    private static final int  PROTOCOL_MSG_STEER = 2;
    private static final int  PROTOCOL_MSG_SIGNALLIGHT = 3;
    private static final int  PROTOCOL_MSG_ACCEL = 4;
    private static final int  PROTOCOL_MSG_BREAK = 5;

    /**
     * Arbitrary constants and variable to track load status. In this example, this variable should
     * only be accessed on the UI thread. In a real app, this variable would be code that performs
     * some UI actions when the video is fully loaded.
     */
    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;
    public static final int TIME_THRESHOLD_SECOND = 1000;
    public static final int TIME_THRESHOLD_FRAME_UPDATE = 200;
    public static final double DEFAULT_SPEED = 0.0;
    public static final double MAX_SPEED = 1.5;
    public static final long REVERSE_TIME = 5000; // 5 seconds
    public static final int GAMEOVER_SCORE = 70;
    public static final int TIME_GAP = 14;

    private int loadVideoStatus;
    private Uri fileUri;    // Tracks the file to be loaded across the lifetime of this app.
    private Options videoOptions = new Options();  // Configuration information for the video.
    private VideoLoaderTask backgroundVideoLoaderTask;
    private VrVideoView mVrVideoView;  // The video view and its custom UI elements.

    //create speed factor to control speed (get it through bluetooth panel)
    private double mSpeed;
    private static int score = 100;           //game score

    //previous values to check changes of factors(speed, turn signal light, steering wheel, accel., break)
    private static double preSpeed;
    private static long currTime = 0;
    private static long prevTime = 0;

    //speed, turn signal light, steering wheel, accel., break
    private static double speed;

    //selection of video
    private static int selectedVideo = 1;
    private static boolean isPlaying = false;
    private static boolean isGameOver = false;

    /**
     * By default, the video will start playing as soon as it is loaded. This can be changed by using
     * {@link VrVideoView#pauseVideo()} after loading the video.
     */
    private boolean isPaused = false;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 자동 꺼짐 방지

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        mBluetoothService = BluetoothService.getInstance();                    // Bluetooth Service
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
        Log.i("TAG", "********Status-Start********");

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
                            Log.i("TAG", "*******" + msg);
                            Log.i("TAG", "*******" + arr.length);
                            Log.i("TAG", "*******" + arr[0]);

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

    private Uri getVideoPATH(String number) {
        Uri path = null;
        switch (number) {
            case Protocol.PLAY_VIDEO_NUMBER1:
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                break;
            case Protocol.PLAY_VIDEO_NUMBER2:
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                break;
            case Protocol.PLAY_VIDEO_NUMBER3:
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                break;
            default:
                Log.w(TAG, "Video number has error. So we should use default value.");
                path = Uri.parse(DEFAULT_VIDEO_NAME);
                break;
        }

        if (USE_ASSET_PATH == false) {
            path = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + DEFAULT_VIDEO_NAME);
        }
        return path;
    }

    private void executeCommand(String[] arr) {
        // msg protocol : cmd////velocity////handle[0,1,2]////signalLight[0,1,2]////accel[0,1]////break[0,1]
        // play||||1,2,3 -> 1,2,3번 동영상 재생
        // stop||||1,2,3 -> 현재 동영상 멈춤
        // cmd|||||40||||90 -> 속도 40, 점수 90
        String cmd = arr[Protocol.INDEX_CMD];
        switch (cmd) {
            case Protocol.CMD_PLAY:
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
                            if(DBG){Log.i(TAG, "chekc - prespeed" + preSpeed);}
                            if(DBG){Log.i(TAG, "chekc - speed" + speed);}
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
                            //getPenalty는 도민이가 정의한 class의 임시명
                            int penalty = TaskValidator.validate(arr, mVrVideoView.getCurrentPosition());
                            if(penalty != 0) {
                                setScore(penalty);
                                if (getScore() < GAMEOVER_SCORE) {
                                    pauseVideo();
                                    isGameOver = true;
                                    Toast.makeText(getApplicationContext(), " 70점 이하 fail!!", Toast.LENGTH_LONG).show();
//                                    String ackMsg = Protocol.CMD_ACK+Protocol.SEPARATOR+Protocol.GAVE_OVER;
                                    mBluetoothService.sendMessage(Protocol.CMD_RESUME);
                                    AlertDialog gameOverDialog = crateGameOverDialog();
                                    gameOverDialog.show();
                                } else {
//                                String ackMsg = Protocol.CMD_ACK+Protocol.SEPARATOR+Protocol.MISSION_FAIl;
                                    mBluetoothService.sendMessage(Protocol.CMD_REWIND);
                                    Toast.makeText(getApplicationContext(), " fail!! 5초전으로 돌림", Toast.LENGTH_SHORT).show();
                                    long videoTime = mVrVideoView.getCurrentPosition();
                                    Log.d(TAG, "[TEST] origin current time : " + videoTime);
                                    if (videoTime > REVERSE_TIME) {
                                        videoTime -= REVERSE_TIME;
                                    } else {
                                        videoTime = 0;
                                    }
                                    Log.d(TAG, "[TEST] before 5second time : " + videoTime);
                                    mVrVideoView.seekTo(videoTime);
                                    currTime = videoTime;
                                    Log.d(TAG, "[TEST] now video time : " + mVrVideoView.getCurrentPosition());
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

    //set speed factor(multiplication of 1000 to make factor as millisecond)
    private void setSpeed(double speed) {
        //0.5가 기본 속도 (0.5배 느림)
        //1이 정상 속도
        //1.5가 빠른 속도 (1.5배 빠름
        //상기 속도에 맞춰서 speed 조절이 필요함

//        if (speed > 0) { // 입력값이 양수
        if (mSpeed >= 0) { // 기존값이 없거나, 있었다고 하면 Default + (speed - 0.1)
            mSpeed = speed;
        }
        if (mSpeed > MAX_SPEED) { // MAX Speed를 초과하지는 못하도록 설정
            mSpeed = MAX_SPEED;
        }
        if (mSpeed < DEFAULT_SPEED) { // Default Speed 보다 작으면 무조건 0으로 설정
            mSpeed = 0;
        }
    }

    public double getSpeedToMilliSecond() {
        return (mSpeed * TIME_THRESHOLD_SECOND);
    }

    public double getAddSpeed() {
        double addSpeed;
        //0.5배 = 70밀리세컨드
        //1배 = 140 밀리세컨드(seekto를 호출하지 않음)
        //1.5배 = 210 밀리세컨드
        addSpeed = mSpeed*10*TIME_GAP;
        return addSpeed;
    }

    public double getSpeed() {
        return mSpeed;
    }

    //set score
    private void setScore(int penalty) {
        score += penalty;
        if (DBG) {
            Log.d(TAG, "[TEST] Change score : " + score);
        }
    }

    //get current score
    public int getScore() {
        return score;
    }

    public void resetScore() {
        score = 100;
    }

    @Override
    public void onBackPressed() {

        LayoutInflater layoutInflater
                = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View popupView = layoutInflater.inflate(popup, null);
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        // PopupWindow 위에서 Button의 Click이 가능하도록 setTouchable(true);
        popupWindow.setTouchable(true);
        // PopupWindow 상의 View의 Button 연결
        Button btnYes = (Button) popupView.findViewById(R.id.btn_yes);
        Button btnNo = (Button) popupView.findViewById(R.id.btn_no);

        if (DBG) {
            Log.i(TAG, "Popup");
        }

        popupWindow.showAtLocation(mVrVideoView, Gravity.CENTER, 0, 0);

        if (DBG) {
            Log.i(TAG, "Popup" + popupWindow.isShowing());
        }

        btnYes.setOnClickListener(new Button.OnClickListener(

        ) {
            @Override
            public void onClick(View view) {
                mBluetoothService.sendMessage(Protocol.CMD_STOP);
                togglePause();
                mVrVideoView.seekTo(0);
                currTime = 0;
                finish();
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
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
        if (DBG) {
            Log.i("TAG", "********Status-Resume********");
        }
        if (mVrVideoView != null) {
            mVrVideoView.resumeRendering(); // Resume the 3D rendering.
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) {
            Log.i("TAG", "********Status-Pause********");
        }
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
        if (DBG) {
            Log.i("TAG", "********Status-Destroy********");
        }
        if (mVrVideoView != null) {
            mVrVideoView.shutdown(); // Destroy the widget and free memory.
        }
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
            if (DBG) {
                Log.i("TAG", "********Status-playvideo********");
            }
            mVrVideoView.playVideo();
            isPlaying = true;
        } else {
            if (DBG) {
                Log.i("TAG", "********Status-pausevideo********");
            }
            mVrVideoView.pauseVideo();
            isPlaying = false;
        }
        isPaused = !isPaused;
    }

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }

    public void reset() {
        //togglePause();
        isGameOver = false;
        Log.d(TAG, "[TEST] ========== RESET ==============");
        mVrVideoView.seekTo(0);
        currTime = 0;
        Log.d(TAG, "[TEST] current position = " + mVrVideoView.getCurrentPosition() );
        resetScore();
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

        long prevCount=0;
        long currCount=0;
        /**
         * Update the UI every frame.
         */
        @Override
        //change onNewFrame for play speed control with speed factor from bluetooth
        public void onNewFrame() {
            if (!isPaused) {
                prevTime = currTime;
                long addSpeed = (long) getAddSpeed();
                Log.i(TAG, "check - videoPosition(speed) = " + addSpeed);
                long videoPosition = mVrVideoView.getCurrentPosition();
                if (speed != 140) {
                    videoPosition = (long)prevTime + addSpeed;
                    mVrVideoView.seekTo(videoPosition);
                }
                Log.i(TAG, "check - videoPosition = " + videoPosition);
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
                //bluetooth에서 select한 video를 가져온다
//                String videoName = getVideo();
//                videoUri = Uri.parse(videoName);

                //test 용 uri
                videoUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + File.separator + DEFAULT_VIDEO_NAME);

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
                    //mVrVideoView.setStereoModeButtonEnabled(false);
                    mVrVideoView.setInfoButtonEnabled(false);
                    mVrVideoView.setFullscreenButtonEnabled(false);
                    //bluetooth에서 select한 video를 가져온다
                    String videoName = DEFAULT_VIDEO_NAME;
                    if (DBG) { Log.d(TAG, "### loadVideoFromAsset()"); }
                    mVrVideoView.loadVideoFromAsset(videoName, videoOptions);
                    //mVrVideoView.loadVideo(videoUri, videoOptions);
                } else {
                    if (DBG) { Log.d(TAG, "### Video path : " + videoUri.toString()); }
                    mVrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    //mVrVideoView.setStereoModeButtonEnabled(false);
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

    private AlertDialog crateGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("GameOver");
        builder.setMessage("다시 시작 하시겠습니까?\nNo를 선택하시면 종료됩니다.");
        //builder.setIcon(R.drawable.);  <-- dialog tile 옆 아이콘

        // msg 는 그저 String 타입의 변수, tv 는 onCreate 메서드에 글을 뿌려줄 TextView
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                reset();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                mBluetoothService.sendMessage(Protocol.CMD_STOP);
                isGameOver = false;
                mVrVideoView.seekTo(0);
                currTime = 0;
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        return dialog;
    }
}
