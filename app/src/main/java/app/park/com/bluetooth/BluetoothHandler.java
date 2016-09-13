package app.park.com.bluetooth;

import android.os.Handler;
import android.os.Message;

import app.park.com.MainActivity;

public class BluetoothHandler extends Handler {
    public static final String TAG = BluetoothFragment.class.getSimpleName();
    public static final boolean DBG = true;

    public interface ActivityCb {
        void sendCbMessage(int msgType, String msg);
    }

    public ActivityCb mActivityCb = null;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;
    private BluetoothService mBluetoothService;

    public BluetoothHandler() {
        mBluetoothService = BluetoothService.getInstance();
    }

    public void setActivityCb(ActivityCb cb) {
        mActivityCb = cb;
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        if (mActivityCb != null) {
                            mActivityCb.sendCbMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_CONNECTED);
                        }
                        MainActivity.updateUi(Constants.MESSAGE_BT_CONNECTED);
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        if (mActivityCb != null) {
                            mActivityCb.sendCbMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_CONNECTING);
                        }
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        if (mActivityCb != null) {
                            mActivityCb.sendCbMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_NONE);
                        }
                        MainActivity.updateUi(Constants.MESSAGE_BT_DISCONNECTED);
                        break;
                }
                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                if (mActivityCb != null) {
                    mActivityCb.sendCbMessage(Constants.MESSAGE_WRITE, writeMessage);
                }
                break;
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if (mActivityCb != null) {
                    mActivityCb.sendCbMessage(Constants.MESSAGE_READ, readMessage);
                }
                break;
            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                if (mActivityCb != null) {
                    mActivityCb.sendCbMessage(Constants.MESSAGE_DEVICE_NAME, mConnectedDeviceName);
                }
                break;
            case Constants.MESSAGE_TOAST:
                if (mActivityCb != null) {
                    mActivityCb.sendCbMessage(Constants.MESSAGE_TOAST, msg.getData().getString(Constants.TOAST));
                }
                break;
        }
    }
}
