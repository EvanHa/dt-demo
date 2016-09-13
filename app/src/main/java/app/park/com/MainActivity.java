package app.park.com;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import app.park.com.bluetooth.BluetoothFragment;
import app.park.com.bluetooth.Constants;
import app.park.com.common.activities.ActivityBase;
import app.park.com.control.ContorlActivity;

public class MainActivity extends ActivityBase implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final boolean DBG = false;

    public static final int ROLE_CONTROLLER = 1;
    public static final int ROLE_VIEWER = 2;

    public static BluetoothFragment mBluetoothFragment; // Bluetooth Control button
    public static int mDeviceRole; // Device Role (Controller or Viewer)

    // Button
    private  static RadioGroup mRoleButton;
    private  static Button mPlayButton;
    private  static Button mPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_button();

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mBluetoothFragment = new BluetoothFragment();
            transaction.replace(R.id.button_fragment, mBluetoothFragment);
            transaction.commit();
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
    public void onClick(View view) {
        String message;
        switch(view.getId()) {
            case R.id.btn_pause:
                if (DBG) {
                    Toast.makeText(getApplicationContext(), "btn_pause", Toast.LENGTH_SHORT).show();
                }
                if (mBluetoothFragment != null) {
                    message = "pause";
                    mBluetoothFragment.sendMessage(message);
                } else {
                    Log.e(TAG, "Can't use bluetooth module");
                }
                break;
            case R.id.btn_play:
                if (DBG) {
                    Toast.makeText(getApplicationContext(), "btn_play", Toast.LENGTH_SHORT).show();
                }
                if (mBluetoothFragment != null) {
                    message = "play";
                    mBluetoothFragment.sendMessage(message);
                    Intent intent = new Intent(getApplicationContext(), ContorlActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "Can't use bluetooth module");
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int id = radioGroup.getCheckedRadioButtonId();
        switch(id) {
            case R.id.radio_controller:
                if (DBG) {
                    Toast.makeText(getApplicationContext(), "radio_controller", Toast.LENGTH_SHORT).show();
                }
                mDeviceRole = ROLE_CONTROLLER;
                break;
            case R.id.radio_viewer:
                if (DBG) {
                    Toast.makeText(getApplicationContext(), "radio_viewer", Toast.LENGTH_SHORT).show();
                }
                mDeviceRole = ROLE_VIEWER;
                break;
        }
    }

    protected void init_button() {
        mRoleButton = (RadioGroup) this.findViewById(R.id.radiogroup);
        mRoleButton.setOnCheckedChangeListener(this);
        mDeviceRole = ROLE_CONTROLLER;

        mPlayButton = (Button) findViewById(R.id.btn_play);
        mPlayButton.setOnClickListener(this);
        mPlayButton.setEnabled(false);

        mPauseButton = (Button) findViewById(R.id.btn_pause);
        mPauseButton.setOnClickListener(this);
        mPauseButton.setEnabled(false);
    }

    public static void updateUi(int msg) {
        switch (msg) {
            case Constants.MESSAGE_BT_CONNECTED:
                if (mDeviceRole == MainActivity.ROLE_CONTROLLER) {
                    mPlayButton.setEnabled(true);
                    mPauseButton.setEnabled(true);
                } else {
                }
                break;

        }
    }
}
