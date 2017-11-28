package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Created by MT on 27/11/2017.
 */

public class RoutesListFragment extends Fragment {
    private ArrayAdapter<String> newRoutesArrayAdapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.routes_list, container, false);

        // Initialize array adapters. One for already paired devices and one for new
        newRoutesArrayAdapter= new ArrayAdapter<String>(getActivity(), R.layout.route_name);

        //todo micaheltah - how to add entries??????????????????????????
        return view;
    }


}
