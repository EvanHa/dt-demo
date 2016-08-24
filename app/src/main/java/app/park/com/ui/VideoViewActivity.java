package app.park.com.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import app.park.com.R;

public class VideoViewActivity extends Activity {
    public static boolean DBG = true;
    public static String TAG = "Client";

    public final int TYPE_URI_RES_PATH  = 1; // 패키지이름/res/video이름
    public final int TYPE_URI_ID_PATH   = 2; // 패키지이름/R.타입.video이름
    public final int TYPE_ABSTRACT_PATH = 3; // 절대경로

    private VideoView mVideoView;
    private MediaController mMediaController;
    private int mPosition=0;  //Video 재생 시작점. 0이면 처음 부터.
    private ProgressDialog mProgressDialog; // loading중 처리용
    private boolean mHiddenNav=false; //software home key 숨김용

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 가로모드, 제목표시줄 제거는 AndroidManifest.xml에 적용
        // activity_video_view.xml의 layout 설정
        setContentView(R.layout.activity_video_view);
        if (DBG) {
            Log.e(TAG, "VideoView Activity onCreate");
        }

        if (mMediaController == null) {
            // MediaController 객체 생성. (동영상 재생시, 재생/정지 등의 컨트롤 바)
            mMediaController = new MediaController(VideoViewActivity.this);
        }

        mVideoView = (VideoView)findViewById(R.id.video_view); //layout에서 VideoView 찾기

        //setProgress(); //loading중 표시

        try {
            mVideoView.setMediaController(mMediaController); //VideoView에 컨트롤러 연결
            mVideoView.setVideoURI(Uri.parse(getVideoPath(TYPE_URI_ID_PATH))); //Video파일 경로 설정
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mPosition", mVideoView.getCurrentPosition());
        mVideoView.pause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPosition = savedInstanceState.getInt("mPosition");
        mVideoView.seekTo(mPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hiddenNavigation(); // soft home key를 사용하는 폰은 이것을 숨겨야 한다.

        mVideoView.requestFocus(); //VideoView에 포커스를 맞춤.
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.seekTo(mPosition);
                if (mPosition==0) {
                    //mProgressDialog.dismiss();
                    mVideoView.start();
                } else {
                    mVideoView.pause();
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        // 화면을 터치 하게 되면, soft home key가 살기 때문에, 클릭에 따라 토글해줘야 한다.
        // 토글은 hiddenNavigation에서 알아서 처리.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            hiddenNavigation();
        }
        return true;
    }

    public void setProgress() {
        // Create a progressbar
        mProgressDialog = new ProgressDialog(VideoViewActivity.this);
        // Set progressbar title
        mProgressDialog.setTitle("Remote Video Control Client");
        // Set progressbar message
        mProgressDialog.setMessage("Loading...");

        mProgressDialog.setCancelable(false);
        // Show progressbar
        mProgressDialog.show();
    }

    public String getVideoPath(int type) {
        String path = "";
        switch(type) {
            case TYPE_URI_RES_PATH:
                path = "android.resource://" + this.getPackageName() + "/res/vid_bigbuckbunny";
                break;
            case TYPE_URI_ID_PATH:
                //path = "android.resource://" + this.getPackageName() + "/" + R.raw.congo;
                break;
            case TYPE_ABSTRACT_PATH:
                break;
            default:
                if (DBG) {
                    Log.e(TAG, "Video Path Type Error");
                }
                break;
        }
        if(DBG) {
            Log.e(TAG, "media path : " + path);
        }
        return path;
    }

    public void hiddenNavigation() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        if (mHiddenNav) {
            mHiddenNav = false;
        } else {
            mHiddenNav = true;
            decorView.setSystemUiVisibility(uiOptions);
        }
/*
        if (decorView.getSystemUiVisibility() != uiOptions) {
            decorView.setSystemUiVisibility(uiOptions);
        }
*/
    }
}
