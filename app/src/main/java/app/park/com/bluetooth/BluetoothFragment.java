/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.park.com.bluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import app.park.com.R;
import app.park.com.vr.VrVideoActivity;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothFragment extends Fragment {
    public static final String TAG = BluetoothFragment.class.getSimpleName();
    public static final boolean DBG = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int BLUETOOTH_DISCOVERY_TIME = 300;

    // Layout Views
    private Button mDiscoverableButton;
    private Button mConnectButton;

    /**
     * Member object for the Bluetooth services
     */
    private BluetoothService mBluetoothService = null;
    private BluetoothHandler mHandler = null;
    private BluetoothHandler.ActivityCb mActivityCb = null;
    private String mConnectedDeviceName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mBluetoothService = BluetoothService.getInstance();
        if ((mBluetoothService != null) && (mBluetoothService.isBluetoothSupported())) {
            if (DBG) { Log.i(TAG, "Bluetooth is supported"); }
        } else {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mDiscoverableButton = (Button) view.findViewById(R.id.btn_bt_discoverable);
        mConnectButton = (Button) view.findViewById(R.id.btn_bt_connect);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupBluetooth() will then be called during onActivityResult
        if (!mBluetoothService.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else { // Otherwise, setup the bt session
            setupBluetooth();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                mBluetoothService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    public void setupBluetooth() {
        if (DBG) { Log.i(TAG, "setupBluetooth()"); }

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mDiscoverableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), BluetoothDeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        // Initialize the BluetoothService to perform bluetooth connections
        if (mHandler == null) {
            mHandler = new BluetoothHandler();
        }
        mActivityCb = new BluetoothHandler.ActivityCb() {
            @Override
            public void sendCbMessage(int msgType, String msg) {
                switch (msgType) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (msg) {
                            case Constants.BLUETOOTH_CONNECTED:
                                setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                                break;
                            case Constants.BLUETOOTH_CONNECTING:
                                setStatus(R.string.title_connecting);
                                break;
                            case Constants.BLUETOOTH_NONE:
                                setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        if (msg.equals("play")) {
                            Toast.makeText(getActivity(), "play", Toast.LENGTH_SHORT).show();
                            startVideoView();
                        } else if (msg.equals("pause")) {
                            Toast.makeText(getActivity(), "pause", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        if (DBG) {
                            Log.i(TAG, msg);
                        }
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        mConnectedDeviceName = msg;
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

    /**
     * Makes this device discoverable.
     */
    public void ensureDiscoverable() {
        if (mBluetoothService.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BLUETOOTH_DISCOVERY_TIME);
            startActivity(discoverableIntent);
        } else {
            Log.w(TAG, "Already set to ensure discoverable");
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When BluetoothDeviceList returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When BluetoothDeviceList returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up bluetooth
                    setupBluetooth();
                } else {
                    // Be rejected using bluetooth
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link BluetoothDeviceList#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(BluetoothDeviceList.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothService.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device, secure);
    }

    public void startVideoView() {
        Intent intent = new Intent(getActivity() , VrVideoActivity.class);
        startActivity(intent);
    }
}
