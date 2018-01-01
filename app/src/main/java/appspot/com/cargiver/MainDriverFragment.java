package appspot.com.cargiver;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by GK on 11/17/2017.
 */

public class MainDriverFragment extends Fragment {

    // progress dialog
    private ProgressDialog mProgressDlg;
    boolean startDrivePressed;
    ImageButton btnStartDrive;
    TextView Explain;
    public DatabaseReference TheRoutesDB;
    String uid;

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
                    synchronized (BluetoothOBDService.class) {
                        MainDriverActivity.btService.stopped = true;
                    };
                    // set parameters to fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", MainDriverActivity.btService.getDriveKey());
                    // stop service
                    getActivity().unbindService(mConnection);
                    getActivity().stopService(intnt);
                    // load result fragment
                    Fragment ShowRouteRes = new RouteResultFragment();
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
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
                    getActivity().bindService(intent, mConnection, 0);
                    getActivity().startService(intnt);
                    //android.os.Debug.waitForDebugger();
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
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothOBDService.BluetoothOBDBinder binder = (BluetoothOBDService.BluetoothOBDBinder) service;
            MainDriverActivity.btService = (BluetoothOBDService) binder.getService();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                public void run() {
                        // wait till connected
                        while (MainDriverActivity.btService!= null && MainDriverActivity.btService.getState() != BluetoothOBDService.STATE_CONNECTED) {

                        }
                        // if we had connected
                        if (MainDriverActivity.btService != null && MainDriverActivity.btService.getState() == BluetoothOBDService.STATE_CONNECTED)
                            changedToActiveDrive();
                        else {
                            mProgressDlg.dismiss();
                            startDrivePressed = false;
                        }

                }
            }, 3000);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // if we disconnected unexpectedly
            if (MainDriverActivity.btService != null) {
                int serviceStatus = MainDriverActivity.btService.getState();
                // stop service
                Intent intnt = new Intent(getActivity(),BluetoothOBDService.class);
                synchronized (BluetoothOBDService.class) {
                    MainDriverActivity.btService.stopped = true;
                };
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
