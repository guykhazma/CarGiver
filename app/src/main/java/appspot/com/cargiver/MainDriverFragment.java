package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by GK on 11/17/2017.
 */

public class MainDriverFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.main_fragment_driver, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(0).setChecked(true);

        getActivity().setTitle("Driver");

        Button btnStartDrive = view.findViewById(R.id.driving_page);

        btnStartDrive.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                //if we want to end the route
                Button tmp = v.findViewById(R.id.driving_page);
                TextView Explain = v.getRootView().findViewById(R.id.start_driving_explain);

                if (tmp.getText().equals("Stop Driving")){
                    Fragment ShowRouteRes = new RouteResultFragment();
                    // set parameters to fragment
                    Bundle bundle = new Bundle();
                    bundle.putString("driveID", "-KzyH3elX37eUbJZNd6l");
                    ShowRouteRes.setArguments(bundle);
                    getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                }
                tmp.setText("Stop Driving");
                Explain.setText("When you arrive your destination, please click on Stop Driving");

            }
        });



        return view;
    }

    protected void StartDrive(View v) {
        //guyk todo insert the function that scan for devices
    }
}
