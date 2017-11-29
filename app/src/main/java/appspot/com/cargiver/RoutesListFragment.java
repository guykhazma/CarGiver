package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
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
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by MT on 27/11/2017.
 */

public class RoutesListFragment extends Fragment {
    public ListView RouteslistView;
    public DatabaseReference TheRoutesDB;
    public RoutesListFragment(){};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.routes_list, container, false);
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(1).setChecked(true);

        //take last 20 routes from db. if less take less...

        //michaeltah - take information from db
        TheRoutesDB = FirebaseDatabase.getInstance().getReference();
        //TheRoutesDB.child("drives");
        //todo michaetlah - see if we need drives list for another use
        final List<Drives> DrivesList = new ArrayList<Drives>(); //will keep the drives data
//        final String[] nameArray; //keep the drivers ids
        RouteslistView = (ListView) view.findViewById(R.id.routes_list_view);

        TheRoutesDB.child("drives").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot Child: dataSnapshot.getChildren()) {
                    DrivesList.add(Child.getValue(Drives.class));
                }
                String[] nameArray = new String[DrivesList.size()];
                for(int i=0; i<DrivesList.size(); i++){
                    nameArray[i] = DrivesList.get(i).currentDriverID;
                }
                RouteListAdapter MyAmazingAdapter = new RouteListAdapter(getActivity(), nameArray, DrivesList);
                RouteslistView.setAdapter(MyAmazingAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        } );

        return view;
    }



}
