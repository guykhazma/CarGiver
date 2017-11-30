package appspot.com.cargiver;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Stav on 27/11/2017.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageDriversFragment extends Fragment{
    private ArrayAdapter<String> listViewAdapter;
    public ManageDriversFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.manage_drivers_fragment, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(2).setChecked(true);

        getActivity().setTitle("Manage Drivers");

        // Get reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid(); // current user id
        // Get list of authorized driver IDs.
        final List<String> driverIDs = new ArrayList<String>();
        dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    driverIDs.add(child.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        // Present list of authorized driver names.
        ListView listView=(ListView)view.findViewById(R.id.listItem);
        listViewAdapter=new ArrayAdapter<String>(
                getActivity(),android.R.layout.simple_list_item_1,
                driverIDs
        );
        listView.setAdapter(listViewAdapter);
        return view;
    }
}
