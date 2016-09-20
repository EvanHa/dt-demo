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
import app.park.com.bluetooth.Protocol;
import app.park.com.control.ControlActivity;

public class VideoFragment extends Fragment {
    public static final String TAG = VideoFragment.class.getSimpleName();
    public static final boolean DBG = false;

    // Layout Views
    private LinearLayout mVideoBtn;
    private Button mButtonVideo1;
    private Button mButtonVideo2;
    private Button mButtonVideo3;
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
        mVideoBtn = (LinearLayout) view.findViewById(R.id.btn_video_group);
        mButtonVideo1 = (Button) view.findViewById(R.id.btn_play1);
        mButtonVideo2 = (Button) view.findViewById(R.id.btn_play2);
        mButtonVideo3 = (Button) view.findViewById(R.id.btn_play3);
    }

    @Override
    public void onStart() {
        super.onStart();
        mBluetoothService = BluetoothService.getInstance();
        initButton();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initButton() {
        mButtonVideo1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = Protocol.CMD_PLAY+Protocol.SEPARATOR+"1";
                mBluetoothService.sendMessage(message);
                startContorlView();
            }
        });

        mButtonVideo2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = Protocol.CMD_PLAY+Protocol.SEPARATOR+"2";
                mBluetoothService.sendMessage(message);
                startContorlView();
            }
        });

        mButtonVideo3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = Protocol.CMD_PLAY+Protocol.SEPARATOR+"3";
                mBluetoothService.sendMessage(message);
                startContorlView();
            }
        });
        disableButton();
    }

    public void setButtonVisible() {
        mVideoBtn.setVisibility(View.VISIBLE);
    }

    public void setButtonInVisible() {
        mVideoBtn.setVisibility(View.INVISIBLE);
    }

    public void enableButton() {
        mButtonVideo1.setEnabled(true);
        mButtonVideo2.setEnabled(true);
        mButtonVideo3.setEnabled(true);
    }

    public void disableButton() {
        mButtonVideo1.setEnabled(false);
        mButtonVideo2.setEnabled(false);
        mButtonVideo3.setEnabled(false);
    }

    public void startContorlView() {
        Intent intent = new Intent(getActivity(), ControlActivity.class);
        startActivity(intent);
    }
}
