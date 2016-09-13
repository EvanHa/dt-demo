package app.park.com.bluetooth;

import android.os.Handler;
import android.os.Message;

public class BluetoothHandler extends Handler {
    public static final String TAG = BluetoothFragment.class.getSimpleName();
    public static final boolean DBG = true;

    interface ActivityCb {
        void sendMessage(int msgType, String msg);
    }

    private ActivityCb mActivityCb = null;

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
                            mActivityCb.sendMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_CONNECTED);
                        }
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        if (mActivityCb != null) {
                            mActivityCb.sendMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_CONNECTING);
                        }
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        if (mActivityCb != null) {
                            mActivityCb.sendMessage(Constants.MESSAGE_STATE_CHANGE, Constants.BLUETOOTH_NONE);
                        }
                        break;
                }
                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                String writeMessage = new String(writeBuf);
                if (mActivityCb != null) {
                    mActivityCb.sendMessage(Constants.MESSAGE_WRITE, writeMessage);
                }
                break;
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if (mActivityCb != null) {
                    mActivityCb.sendMessage(Constants.MESSAGE_READ, readMessage);
                }
                break;
            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                if (mActivityCb != null) {
                    mActivityCb.sendMessage(Constants.MESSAGE_DEVICE_NAME, mConnectedDeviceName);
                }
                break;
            case Constants.MESSAGE_TOAST:
                if (mActivityCb != null) {
                    mActivityCb.sendMessage(Constants.MESSAGE_TOAST, msg.getData().getString(Constants.TOAST));
                }
                break;
        }
    }
}
