package appspot.com.cargiver;
//package com.mkyong.android;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Guybb96 on 11/27/2017.
 */

public class ManageSupervisorsFragment extends Fragment {
    public ManageSupervisorsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String[] names = { "Apple","it","Jackfruit", "Mango", "Olive", "Pear", "Sugar-apple" };
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.driver_manage_supervisors_fragment, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(2).setChecked(true);

        getActivity().setTitle("Manage Supervisors");

        ListView listView=(ListView)view.findViewById(R.id.listItem);
        ArrayAdapter<String> listViewAdapter=new ArrayAdapter<String>(
            getActivity(),android.R.layout.simple_list_item_1,
            names
        );
        listView.setAdapter(listViewAdapter);

        return view;
    }
    public void AddSupervisor(View v)
    {
      //pop dialog to enter email of supervisor and if exists put this supervisor
    }
}
