package app.park.com.vr;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import app.park.com.R;
import app.park.com.bluetooth.BluetoothService;
import app.park.com.control.ContorlActivity;

public class VideoFragment extends Fragment {
    public static final String TAG = VideoFragment.class.getSimpleName();
    public static final boolean DBG = true;

    // Layout Views
    private LinearLayout mVideoBtn;
    private Button mPlayButton;
    private Button mPauseButton;
    private BluetoothService mBluetoothService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mVideoBtn = (LinearLayout) view.findViewById(R.id.btn_video);
        mPlayButton = (Button) view.findViewById(R.id.btn_play);
        mPauseButton = (Button) view.findViewById(R.id.btn_pause);
    }

    @Override
    public void onStart() {
        super.onStart();
        mBluetoothService = BluetoothService.getInstance();
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = "play";
                mBluetoothService.sendMessage(message);
                Intent intent = new Intent(getActivity(), ContorlActivity.class);
                startActivity(intent);
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = "puase";
                mBluetoothService.sendMessage(message);
            }
        });
        mPlayButton.setEnabled(false);
        mPauseButton.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setButtonVisible() {
        mVideoBtn.setVisibility(View.VISIBLE);
    }

    public void setButtonInVisible() {
        mVideoBtn.setVisibility(View.INVISIBLE);
    }

    public void enableButton() {
        mPlayButton.setEnabled(true);
        mPauseButton.setEnabled(true);
    }

    public void disableButton() {
        mPlayButton.setEnabled(false);
        mPauseButton.setEnabled(false);
    }
}
