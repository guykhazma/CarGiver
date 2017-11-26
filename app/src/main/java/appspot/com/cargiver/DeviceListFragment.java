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
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

    // progress bar for search
    private ProgressDialog mProgressDlg;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_list_fragment, container, false);
        // Initialize the button to perform device discovery
        Button scanButton = (Button) view.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
            }
        });

        // Initialize array adapters. One for already paired devices and one for new
        newDevicesArrayAdapter= new ArrayAdapter<String>(getActivity(), R.layout.device_name);
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) view.findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) view.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

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

        // Inflate the layout for this fragment
        return view;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle("Bluetooth Scan");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Disable the floating button to avoid opening multiple
        final FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setEnabled(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Enable the floating button to avoid opening multiple
        final FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setEnabled(true);
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
                if (device.getName().equals(BluetoothOBDService.dev.getName()))
                    pairedDevicesArrayAdapter.add(device.getName() + " - Connected\n" + device.getAddress());
                else
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

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            if (!BluetoothOBDService.isConnecting) {
                if (BluetoothOBDService.dev == null) {
                    // Connect
                    Log.d(TAG, "connect to: " + info + " " + address);
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                    // Start the thread to connect with the given device
                    BluetoothOBDService.connect(device, true);
                }
                else if (BluetoothOBDService.dev.getAddress() != address) {
                    // Connect
                    Log.d(TAG, "connect to: " + info + " " + address);
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                    // Start the thread to connect with the given device
                    BluetoothOBDService.connect(device, true);
                }
                else {
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Already connected to " + BluetoothOBDService.dev.getName(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            else {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Connection already in action", Toast.LENGTH_SHORT);
                toast.show();
            }


        }
    };

    public static BluetoothSocket connect(BluetoothDevice dev) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback = null;

        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            sock = dev.createRfcommSocketToServiceRecord(MY_UUID);
            sock.connect();
        } catch (Exception e1) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            Class<?> clazz = sock.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                sockFallback.connect();
                sock = sockFallback;
            } catch (Exception e2) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                throw new IOException(e2.getMessage());
            }
        }
        return sock;
    }

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
     * Extracts string list from the device list we got from main_driver activity
     * @param btArray
     * @return
     */
    private ArrayList<String> getDeviceNames(ArrayList<BluetoothDevice> btArray) {
        ArrayList<String> devices = new ArrayList<String>();
        if (btArray.size() > 0) {
            for (BluetoothDevice bt : btArray) {
                devices.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else
            devices.add("No Bluetooth devices found");
        return devices;
    }

}
