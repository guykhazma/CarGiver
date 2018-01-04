package appspot.com.cargiver;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by GK on 11/17/2017.
 */

public class MainDriverFragment extends Fragment {

    // progress dialog
    private ProgressDialog mProgressDlg;
    boolean startDrivePressed;
    ImageButton btnStartDrive;
    TextView Explain;

    // Our handler for received Intents. This will be called whenever an Intent
    private BroadcastReceiver mreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothOBDService.connectionFailedBroadcastIntent.equals(action)) {
                // if service failed to connect to OBD
                MainDriverActivity.btService = null;
                startDrivePressed = false;
                if (mProgressDlg != null) {
                    mProgressDlg.dismiss();
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Failed to Connect to OBD", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // connection lost case
            else if (BluetoothOBDService.connectionLostBroadcastIntent.equals(action)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getActivity(), "Connection with OBD is lost", Toast.LENGTH_SHORT).show();
                    }
                });
                // set parameters to fragment
                String driveID = MainDriverActivity.btService.getDriveKey();
                // if there is internet and drive has finished load result
                if (driveID != null && isNetworkAvailable()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", driveID);
                    // load result fragment
                    Fragment ShowRouteRes = new RouteResultFragment();
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                } else {
                    startDrivePressed = false;
                    btnStartDrive.setImageResource(R.drawable.startdriving);
                    Explain.setText("Click \'Start Driving\' to start the data collection");
                    Toast.makeText(getActivity(), "Drive has finished", Toast.LENGTH_SHORT).show();
                }

            }
            // connected
            else if (BluetoothOBDService.connectionConnectedBroadcastIntent.equals(action)) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        changedToActiveDrive();
                        Toast toast = Toast.makeText(getActivity(), "Connected successfully to " + MainDriverActivity.bluetoothDevice.getName() , Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        // connection changes
        filter.addAction(BluetoothOBDService.connectionFailedBroadcastIntent);
        filter.addAction(BluetoothOBDService.connectionConnectedBroadcastIntent);
        filter.addAction(BluetoothOBDService.connectionLostBroadcastIntent);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mreceiver, filter);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.main_fragment_driver, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(0).setChecked(true);

        btnStartDrive = view.findViewById(R.id.btn_start_drive);
        Explain = view.findViewById(R.id.start_driving_explain);

        //michaeltah - set title
        String usr = null;
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser.getDisplayName()!=null && !currentUser.getDisplayName().equals("")) {
            StringBuffer MyUserName = new StringBuffer("Hello ");
            MyUserName.append(currentUser.getDisplayName());
            getActivity().setTitle(MyUserName);
        }
        else {
            // try getting data from provider
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (usr == null && userInfo.getDisplayName() != null) {
                    usr = userInfo.getDisplayName();
                }
            }
            if (usr != null && !usr.equals("")) {
                getActivity().setTitle("Hello " + usr);
            } else {
                getActivity().setTitle("Hello Driver");
            }
        }

        // bind to service
        // Bind to LocalService if exists
        Intent intent = new Intent(getActivity(), BluetoothOBDService.class);
        getActivity().bindService(intent, mConnection, 0);

        // make sure to display the right option if data collection already started
        if (MainDriverActivity.btService != null && MainDriverActivity.btService.getState() == BluetoothOBDService.STATE_CONNECTED) {
            btnStartDrive.setImageResource(R.drawable.havearrived);
            Explain.setText("When you arrive at your destination, please click on \'I have arrived\'");
            startDrivePressed = true;
        }

        btnStartDrive = view.findViewById(R.id.btn_start_drive);

        btnStartDrive.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                //if we want to end the route
                btnStartDrive = v.findViewById(R.id.btn_start_drive);
                Explain = getView().findViewById(R.id.start_driving_explain);
                if (startDrivePressed){
                    // stop service
                    Intent intnt = new Intent(getActivity(),BluetoothOBDService.class);
                    MainDriverActivity.btService.stopped = true;
                    // stop service
                    getActivity().unbindService(mConnection);
                    getActivity().stopService(intnt);
                    // set parameters to fragment
                    String driveID = MainDriverActivity.btService.getDriveKey();
                    MainDriverActivity.btService.mState = BluetoothOBDService.STATE_NONE;
                    // broadcast finish
                    Intent intent = new Intent(BluetoothOBDService.driveFinishedBroadcastIntent);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcastSync(intent);
                    // if there is internet and drive has finished load result
                    if (driveID != null && isNetworkAvailable()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("driveID", driveID);
                        // load result fragment
                        Fragment ShowRouteRes = new RouteResultFragment();
                        ShowRouteRes.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                    } else {
                        btnStartDrive.setImageResource(R.drawable.startdriving);
                        Explain.setText("Click \'Start Driving\' to start the data collection");
                        Toast.makeText(getActivity(), "Drive has finished", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    // make sure bluetooth is on
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Please Enable Bluetooth", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    // make sure bluetooth device was selected
                    if (MainDriverActivity.bluetoothDevice== null) {
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Please select Bluetooth device", Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }
                    mProgressDlg = new ProgressDialog(getActivity());
                    mProgressDlg.setMessage("Starting Data Collection Engine...");
                    mProgressDlg.setCancelable(false);
                    mProgressDlg.show();
                    // start service
                    Intent intnt = new Intent(getActivity(),BluetoothOBDService.class);
                    intnt.putExtra("address", MainDriverActivity.bluetoothDevice.getAddress());
                    intnt.putExtra("userID", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    Intent intent = new Intent(getActivity(), BluetoothOBDService.class);
                    BluetoothOBDService.numRestart = 0;
                    BluetoothOBDService.restart = false;
                    getActivity().bindService(intent, mConnection, 0);
                    getActivity().startService(intnt);
                }
                // register press
                startDrivePressed = !startDrivePressed;
            }
        });
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mreceiver);
        getActivity().unbindService(mConnection);
    }

    // Will be called by activity on drive start
    public void changedToActiveDrive() {
        btnStartDrive.setImageResource(R.drawable.havearrived);
        Explain.setText("When you arrive at your destination, please click on \'I have arrived\'");
        // dismiss progress bar if we came from pressing the button
        if (mProgressDlg != null) {
            mProgressDlg.dismiss();
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothOBDService.BluetoothOBDBinder binder = (BluetoothOBDService.BluetoothOBDBinder) service;
            MainDriverActivity.btService = (BluetoothOBDService) binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // if we disconnected unexpectedly
            if (MainDriverActivity.btService != null) {
                int serviceStatus = MainDriverActivity.btService.getState();
                // stop service
                Intent intnt = new Intent(getActivity(),BluetoothOBDService.class);
                MainDriverActivity.btService.stopped = true;
                getActivity().unbindService(this);
                getActivity().stopService(intnt);
                // if the service stopped after it was connected go to result fragment
                if (MainDriverActivity.btService.getDriveKey() != null) {
                    Fragment ShowRouteRes = new RouteResultFragment();
                    // set parameters to fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", MainDriverActivity.btService.getDriveKey());
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                }
            }
            // if service disconnected during connection
            MainDriverActivity.btService = null;
            startDrivePressed = false;
            mProgressDlg.dismiss();
        }
    };
}
