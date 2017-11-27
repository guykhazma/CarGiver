package appspot.com.cargiver;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by MT on 27/17/2017.
 */

public class RouteResultFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    String driveID;
    MapView mapView;
    GoogleMap googleMap;
    DatabaseReference dbRef;
    Drives drive; // will save current drive

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment - build the actual display
        View view =  inflater.inflate(R.layout.display_route_result, container, false);
        final TextView txtStart = (TextView) view.findViewById(R.id.time_started);
        final TextView txtEnd = (TextView) view.findViewById(R.id.time_finished);
        getActivity().setTitle("Route Result");

        // init db
        dbRef = FirebaseDatabase.getInstance().getReference();

        // get drive id
        driveID = getArguments().getString("driveID");

        final ChildEventListener updateMap = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "failed loading measurment", databaseError.toException());
                // ...
            }
        };


        ValueEventListener loadData = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                drive = dataSnapshot.getValue(Drives.class);

                txtStart.setText(drive.getStartTime().toString());
                if (drive.ongoing) {
                    txtEnd.setText("Drive Is Active");
                }
                else {
                    if (drive.ongoing) {
                        dbRef.child("drives").child(driveID).child("meas").addChildEventListener(updateMap);
                    }
                    txtEnd.setText(drive.getEndTime().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "failed loading drive", databaseError.toException());
            }
        };
        // load data initial data
        dbRef.child("drives").child(driveID).addListenerForSingleValueEvent(loadData);


        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        if (mapView != null) {
            //googleMap = mapView.getMapAsync(this.onMapReady);
        }


        return view;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        //map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    public void onResume(){
        super.onResume();
    }



}
