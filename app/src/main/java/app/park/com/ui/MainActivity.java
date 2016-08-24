package app.park.com.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ViewAnimator;

import app.park.com.bluetooth.BluetoothFragment;
import app.park.com.R;
import app.park.com.bluetooth.Constants;
import app.park.com.common.activities.ActivityBase;
import app.park.com.control.ContorlActivity;
import app.park.com.vr.VrVideoActivity;

public class MainActivity extends ActivityBase implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int ROLE_CONTROLLER = 1;
    public static final int ROLE_VIEWER = 2;

    static public BluetoothFragment mBT_Fragment;
    static public int mDeviceRole;
    // Button
    static private  RadioGroup mRoleButton;
    static private  Button mPlayButton;
    static private  Button mPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_button();

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            mBT_Fragment = new BluetoothFragment();
            transaction.replace(R.id.button_fragment, mBT_Fragment);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        }
        return false;
    }

    @Override
    public void onClick(View view) {
        String message;
        switch(view.getId()) {
            case R.id.btn_pause:
                Toast.makeText(getApplicationContext(), "btn_pause", Toast.LENGTH_SHORT).show();
                message = "pause";
                mBT_Fragment.sendMessage(message);
                break;
            case R.id.btn_play:
                Toast.makeText(getApplicationContext(), "btn_play", Toast.LENGTH_SHORT).show();
                message = "play";
                mBT_Fragment.sendMessage(message);
                Intent intent = new Intent(getApplicationContext() , ContorlActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        int id = radioGroup.getCheckedRadioButtonId();
        switch(id) {
            case R.id.radio_controller:
                Toast.makeText(getApplicationContext(), "radio_controller", Toast.LENGTH_SHORT).show();
                mDeviceRole = ROLE_CONTROLLER;
                break;
            case R.id.radio_viewer:
                Toast.makeText(getApplicationContext(), "radio_viewer", Toast.LENGTH_SHORT).show();
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

    public int getDeviceRole() {
        return this.mDeviceRole;
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
