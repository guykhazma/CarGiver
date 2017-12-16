package appspot.com.cargiver;

import android.app.Fragment;
import android.app.ProgressDialog;
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

/**
 * Created by GK on 11/17/2017.
 */

public class MainDriverFragment extends Fragment {

    // progress dialog
    private ProgressDialog mProgressDlg;
    boolean startDrivePressed;
    ImageButton btnStartDrive;
    TextView Explain;

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

        getActivity().setTitle("Driver");

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
                        BluetoothOBDService.stopped = true;
                    };
                    getActivity().unbindService(mConnection);
                    getActivity().stopService(intnt);
                    Fragment ShowRouteRes = new RouteResultFragment();
                    // set parameters to fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", BluetoothOBDService.getDriveKey());
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                }
                else {
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
                    // update drive fragment
                    changedToActiveDrive();
                }
            }, 2000);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            MainDriverActivity.btService = null;
        }
    };
}
