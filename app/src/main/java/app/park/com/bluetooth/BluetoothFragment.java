package app.park.com.bluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import app.park.com.MainActivity;
import app.park.com.R;
import app.park.com.vr.VrVideoActivity;

/**
 * 블루투스 관련 버튼 및 UI 처리 Fragment
 */
public class BluetoothFragment extends Fragment {
    public static final String TAG = BluetoothFragment.class.getSimpleName();
    public static final boolean DBG = false;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1; // 블루투스 연결 옵션
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2; // 블루투스 연결 옵션
    private static final int REQUEST_ENABLE_BT = 3; // 블루투스 사용 요청
    private static final int BLUETOOTH_DISCOVERY_TIME = 300; // 블루투스 디바이스로 검색 가능하도록 설정 최대시간

    // Layout Views
    private Button mDiscoverableButton;
    private Button mConnectButton;
    private Button mDisConnectButton;

    /**
     * Member object for the Bluetooth services
     */
    private BluetoothService mBluetoothService = null; // 블루투스 서비스
    private BluetoothHandler mHandler = null; // 블루투스 핸들러
    private BluetoothHandler.ActivityCb mActivityCb = null; // UI 콜백
    private String mConnectedDeviceName = null; // 블루투스로 연결된 디바이스 이름
    private boolean isBluetoothEnabled = false; // 블루투스가 활성화 되어있는지 확인

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mBluetoothService = BluetoothService.getInstance(); // 블루투스 서비스를 제어하는 인스턴스
        if ((mBluetoothService != null) && (mBluetoothService.isBluetoothSupported())) {
            // 블루투스를 지원하는 기기인 경우
            if (DBG) { Log.i(TAG, "Bluetooth is supported"); }
        } else {
            // 블루투스를 지원하지 않는 기기인 경우
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, R.string.error_not_supported_bluetooth, Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false); // 블루투스 버튼 화면을 관리
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mDiscoverableButton = (Button) view.findViewById(R.id.btn_bt_discoverable); // 블루투스 디바이스로 검색가능하게 하는 버튼
        mConnectButton = (Button) view.findViewById(R.id.btn_bt_connect); // 블루투스 기기 연결을 위한 버튼
        mDisConnectButton = (Button) view.findViewById(R.id.btn_bt_disconnect); //블루투스 기기 연결을 끊기 위한 버튼
    }

    @Override
    public void onStart() {
        super.onStart();

        // 블루투스가 활성화 되지 않은경우, 블루투스를 활성화 시키도록 질의하는 팝업 생성
        isBluetoothEnabled = mBluetoothService.isEnabled(); // 블루투스가 활성화 되어있는지 확인
        if (isBluetoothEnabled == false) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else { // 이미 활성화 된 경우, 블루투스 설정
            setupBluetooth();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 블루투스 서비스가 정상동작 하는지 체크
        if (mBluetoothService != null) {
            if ((isBluetoothEnabled==true) && (mBluetoothService.getState() == BluetoothService.STATE_NONE)) {
                // 블루투스 사용준비가 된경우, 서비스를 시작한다.
                mBluetoothService.start();
            }
        } else { // 블루투스 서비스가 null인경우 다시 instance를 얻는다.
            mBluetoothService = null;
            mBluetoothService = BluetoothService.getInstance();
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

        mDisConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                    mBluetoothService.stop();
                }
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
                                MainActivity.updateUi(Constants.MESSAGE_BT_CONNECTED);
                                break;
                            case Constants.BLUETOOTH_CONNECTING:
                                setStatus(R.string.title_connecting);
                                break;
                            case Constants.BLUETOOTH_NONE:
                                setStatus(R.string.title_not_connected);
                                MainActivity.updateUi(Constants.MESSAGE_BT_DISCONNECTED);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_READ:
                        if (DBG) {
                            Log.i(TAG, "[read]" + msg);
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        if (DBG) {
                            Log.i(TAG, "[write]" + msg);
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
                    isBluetoothEnabled = mBluetoothService.isEnabled();
                    if (isBluetoothEnabled == true) {
                        mBluetoothService.start();
                    }
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

    public boolean isBluetoothConnected() {
        boolean result = false;
        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            result = true;
        }
        return result;
    }

    public void updateStatus() {
        int state = mBluetoothService.getState();
        switch (state) {
            case BluetoothService.STATE_CONNECTED:
                setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                break;
            case BluetoothService.STATE_CONNECTING:
                setStatus(R.string.title_connecting);
                break;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
                setStatus(R.string.title_not_connected);
                break;
            default:
        }
    }
}
