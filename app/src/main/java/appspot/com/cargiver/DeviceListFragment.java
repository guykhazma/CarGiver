package appspot.com.cargiver;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by GK on 11/17/2017.
 * adapted from https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/DeviceListActivity.java
 */

public class DeviceListFragment extends Fragment {
    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceListFragment";
    /**
     * Bluetooth UUID
     */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;
    /**
     * Devices
     */
    private ArrayAdapter<String> newDevicesArrayAdapter;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ListView pairedListView;
    private ListView newDevicesListView;
    private TextView txtPaired;
    private TextView txtNew;
    private ImageView obdImage;

    // progress bar for search
    private ProgressDialog mProgressDlg;
    private TextView selectedDevice;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_list_fragment, container, false);
        Button scanButton = (Button) view.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
            }
        });

        // Initialize array adapters. One for already paired devices and one for new
        newDevicesArrayAdapter= new ArrayAdapter<String>(getActivity(), R.layout.device_name);
        //device_name - this is every entry in the list
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) view.findViewById(R.id.paired_devices);
        pairedListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        newDevicesListView = (ListView) view.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // load text views and image
        txtPaired = (TextView) view.findViewById(R.id.title_paired_devices);
        txtNew = (TextView) view.findViewById(R.id.title_new_devices);
        obdImage = (ImageView) view.findViewById(R.id.img_OBD);

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);

        // progress bar for bluetooth scan
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Cancel Discovery
                mBtAdapter.cancelDiscovery();
            }
        });

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            view.findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        // set selected device label label
        selectedDevice = (TextView) view.findViewById(R.id.selected_device);
        // if no device was selected
        if (MainDriverActivity.bluetoothDevice == null) {
            selectedDevice.setText("Please select your OBD device");
            pairedListView.setVisibility(View.VISIBLE);
            newDevicesListView.setVisibility(View.VISIBLE);
            txtPaired.setVisibility(View.VISIBLE);
            txtNew.setVisibility(View.VISIBLE);
            obdImage.setVisibility(View.GONE);
        }
        else {
            selectedDevice.setText("Selected OBD Device Name:\n" + MainDriverActivity.bluetoothDevice.getName());
            pairedListView.setVisibility(View.GONE);
            newDevicesListView.setVisibility(View.GONE);
            txtPaired.setVisibility(View.GONE);
            txtNew.setVisibility(View.GONE);
            obdImage.setVisibility(View.VISIBLE);

        }
        // Inflate the layout for this fragment
        return view;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle("Bluetooth Manager");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        //Unregister broadcast listeners
        this.getActivity().unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        mProgressDlg.show();
        getActivity().setTitle(R.string.scanning);
        pairedListView.setVisibility(View.VISIBLE);
        newDevicesListView.setVisibility(View.VISIBLE);
        txtPaired.setVisibility(View.VISIBLE);
        txtNew.setVisibility(View.VISIBLE);
        obdImage.setVisibility(View.GONE);

        // clear recent found devices
        newDevicesArrayAdapter.clear();
        pairedDevicesArrayAdapter.clear();

        // refill paired devices
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            getView().findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Set device for bluetooth OBD service
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            MainDriverActivity.bluetoothDevice =  mBtAdapter.getRemoteDevice(address);

            // set connection label
            selectedDevice.setText("Selected OBD Device Name:\n" + MainDriverActivity.bluetoothDevice.getName());

            // save preference
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(FirebaseAuth.getInstance().getCurrentUser().getUid(), address);
            editor.commit();
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Discovery Finished");
                getActivity().setTitle("Bluetooth devices");
                mProgressDlg.dismiss();
                if (newDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };


    /**
     * The Handler that gets information back from the BluetoothChatService

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothOBDService.MESSAGE_STATE_CHANGE:
                    TextView connectedTo = (TextView) getView().findViewById(R.id.connected_to);
                    switch (BluetoothOBDService.status) {
                        case BluetoothOBDService.STATE_CONNECTED:
                            connectedTo.setText("Connected to " + BluetoothOBDService.dev.getName());
                            connectedTo.setBackgroundColor(Color.parseColor("#4CAF50"));
                            break;
                        case BluetoothOBDService.STATE_CONNECTING:
                            connectedTo.setText("Connecting...");
                            break;
                        case BluetoothOBDService.STATE_DISCONNECTED:
                            connectedTo.setText("Not connected");
                            connectedTo.setBackgroundColor(Color.parseColor("#DD2C00"));
                            break;
                    }
            }
        }
    };*/

}
