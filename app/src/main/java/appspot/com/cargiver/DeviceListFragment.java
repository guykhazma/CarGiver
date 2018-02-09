package appspot.com.cargiver;

import android.app.Activity;
import android.app.AlertDialog;
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

        //device_name - this is every entry in the list
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name);

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) view.findViewById(R.id.paired_devices);
        pairedListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);


        // load text views and image
        txtPaired = (TextView) view.findViewById(R.id.title_paired_devices);
        obdImage = (ImageView) view.findViewById(R.id.img_OBD);

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
            selectedDevice.setText("Please select your OBD device\nClick on the OBD for Help");
        }
        else {
            selectedDevice.setText("Selected OBD Device Name:\n" + MainDriverActivity.bluetoothDevice.getName());
        }

        // help
        ImageView obdImg = (ImageView) view.findViewById(R.id.img_OBD);
        obdImg.setClickable(true);
        obdImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder DriverHlp = new AlertDialog.Builder(getActivity());
                DriverHlp.setTitle("OBD Setup Help");
                DriverHlp.setMessage("The OBD device doesn't appear?\nPlease pair it for the first time using the Bluetooth scan screen\nif the device require password try 0000 or 1234\nAfter the pairing reload the page and select it");
                DriverHlp.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                DriverHlp.create();
                DriverHlp.show();
            }
        });
        // Inflate the layout for this fragment
        return view;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle("OBD Manager");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
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

}
