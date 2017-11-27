package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by MT on 27/17/2017.
 */

public class RouteResultFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment - build the actual display
        View view =  inflater.inflate(R.layout.display_route_result, container, false);

        getActivity().setTitle("Route Result");

        return view;
    }


}
