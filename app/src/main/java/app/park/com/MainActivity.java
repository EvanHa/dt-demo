package app.park.com;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import app.park.com.bluetooth.BluetoothFragment;
import app.park.com.bluetooth.Constants;
import app.park.com.common.activities.ActivityBase;
import app.park.com.vr.VideoFragment;

public class MainActivity extends ActivityBase implements RadioGroup.OnCheckedChangeListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean DBG = false;

    public static final int ROLE_CONTROLLER = 1;
    public static final int ROLE_VIEWER = 2;

    public static BluetoothFragment mBluetoothFragment; // Bluetooth Control button
    public static VideoFragment mVideoFragment; // Video Button
    public static int mDeviceRole; // Device Role (Controller or Viewer)
    private static RadioGroup mRoleButton; // Device Role Radio Button

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
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                if (mBluetoothFragment.isBluetoothConnected()) {
                    updateUi(Constants.MESSAGE_BT_CONNECTED);
                }
                break;
            case R.id.radio_viewer:
                if (DBG) {
                    Log.d(TAG, "radio_viewer");
                }
                mDeviceRole = ROLE_VIEWER;
                mVideoFragment.setButtonInVisible();
                break;
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
                } else {
                }
                break;
            case Constants.MESSAGE_BT_DISCONNECTED:
            default:
                mVideoFragment.disableButton();
                break;
        }
    }
}
