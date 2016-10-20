package app.park.com;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RadioGroup;
import app.park.com.bluetooth.BluetoothFragment;
import app.park.com.bluetooth.Constants;
import app.park.com.common.activities.ActivityBase;
import app.park.com.vr.VideoFragment;
import app.park.com.vr.VrVideoActivity;

public class MainActivity extends ActivityBase implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean DBG = false;

    public static final int ROLE_CONTROLLER = 1;
    public static final int ROLE_VIEWER = 2;

    private static BluetoothFragment mBluetoothFragment; // Bluetooth Control button
    private static VideoFragment mVideoFragment; // Video Button
    private static int mDeviceRole; // Device Role (Controller or Viewer)
    private static RadioGroup mRoleButton; // Device Role Radio Button
    private static MainActivity mMainInstatnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_button();

        if (savedInstanceState == null) {
            FragmentTransaction btTransaction = getSupportFragmentManager().beginTransaction();
            mBluetoothFragment = new BluetoothFragment();
            btTransaction.replace(R.id.button_bluetooth_fragment, mBluetoothFragment);
            btTransaction.commit();

            FragmentTransaction videoTransaction = getSupportFragmentManager().beginTransaction();
            mVideoFragment = new VideoFragment();
            videoTransaction.replace(R.id.button_video_fragment, mVideoFragment);
            videoTransaction.commit();
        }
        mMainInstatnce = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mBluetoothFragment.isBluetoothConnected()) {
            updateUi(Constants.MESSAGE_BT_CONNECTED);
        } else {
            updateUi(Constants.MESSAGE_BT_DISCONNECTED);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int id = radioGroup.getCheckedRadioButtonId();
        switch(id) {
            case R.id.radio_controller:
                if (DBG) {
                    Log.d(TAG, "radio_controller");
                }
                mDeviceRole = ROLE_CONTROLLER;
                mVideoFragment.setButtonVisible();
                break;
            case R.id.radio_viewer:
                if (DBG) {
                    Log.d(TAG, "radio_viewer");
                }
                mDeviceRole = ROLE_VIEWER;
                mVideoFragment.setButtonInVisible();
                break;
        }
        if (mBluetoothFragment.isBluetoothConnected()) {
            updateUi(Constants.MESSAGE_BT_CONNECTED);
        }
    }

    protected void init_button() {
        mRoleButton = (RadioGroup) this.findViewById(R.id.radiogroup);
        mRoleButton.setOnCheckedChangeListener(this);
        mDeviceRole = ROLE_CONTROLLER; // Default role is controller
    }

    public static void updateUi(int msg) {
        switch (msg) {
            case Constants.MESSAGE_BT_CONNECTED:
                if (mDeviceRole == MainActivity.ROLE_CONTROLLER) {
                    mVideoFragment.enableButton();
                } else { // Device role is viewer
                    mMainInstatnce.startVideoView();
                }
                break;
            case Constants.MESSAGE_BT_DISCONNECTED:
            default:
                mVideoFragment.disableButton();
                break;
        }
        mBluetoothFragment.updateStatus();
    }

    public void startVideoView() {
        Intent intent = new Intent(getApplicationContext() , VrVideoActivity.class);
        startActivity(intent);
    }
}
