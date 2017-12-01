package appspot.com.cargiver;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by GK on 11/17/2017.
 */

public class MainDriverFragment extends Fragment {

    boolean startDrivePressed;
    // progress dialog
    private ProgressDialog mProgressDlg;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.main_fragment_driver, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(0).setChecked(true);

        ImageButton btnStartDrive = view.findViewById(R.id.btn_start_drive);
        TextView Explain = view.findViewById(R.id.start_driving_explain);

        // make sure to display the right option if data collection already started
        if (startDrivePressed == true) {
            btnStartDrive.setImageResource(R.drawable.havearrived);
            Explain.setText("When you arrive at your destination, please click on \'I have arrived\'");
        }

        getActivity().setTitle("Driver");

        btnStartDrive = view.findViewById(R.id.btn_start_drive);

        btnStartDrive.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                //if we want to end the route
                ImageButton btnStartDrive = v.findViewById(R.id.btn_start_drive);
                TextView Explain = getView().findViewById(R.id.start_driving_explain);
                if (startDrivePressed){
                    Fragment ShowRouteRes = new RouteResultFragment();
                    // set parameters to fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", "-KzyH3elX37eUbJZNd6l");
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                }
                else {
                    mProgressDlg = new ProgressDialog(getActivity());
                    mProgressDlg.setMessage("Starting Data Collection Engine...");
                    mProgressDlg.setCancelable(false);
                    mProgressDlg.show();
                    btnStartDrive.setImageResource(R.drawable.havearrived);
                    Explain.setText("When you arrive at your destination, please click on \'I have arrived\'");
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            mProgressDlg.dismiss();
                        }
                    }, 2000);
                }
                // register press
                startDrivePressed = !startDrivePressed;
            }
        });



        return view;
    }

    protected void StartDrive(View v) {
        //guyk todo insert the function that scan for devices
    }
}
