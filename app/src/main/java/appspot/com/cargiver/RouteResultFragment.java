package appspot.com.cargiver;

import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.Speedometer;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by MT on 27/17/2017.
 */

public class RouteResultFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    DatabaseReference dbRef;

    // data loaded from db
    String driveID;
    Drives drive; // will save current drive
    String driver;
    String supervisor;

    // view objects
    TextView txtStart;
    TextView txtEnd;
    TextView driverName;
    TextView rating;
    Speedometer speedometer;

    // progress dialog
    private ProgressDialog mProgressDlg;

    // map object
    MapView mMapView;
    GoogleMap googleMap;
    Polyline path = null;
    PolylineOptions pathOptions = null;
    LatLngBounds.Builder mapBuilder = null;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment - build the actual display
        View view =  inflater.inflate(R.layout.display_route_result, container, false);
        // init view objects
        txtStart = (TextView) view.findViewById(R.id.time_started);
        txtEnd = (TextView) view.findViewById(R.id.time_finished);
        driverName = (TextView) view.findViewById(R.id.driver_name);
        speedometer = (Speedometer)  view.findViewById(R.id.speedView);
        rating = (TextView) view.findViewById(R.id.rating);

        // show loading
        hideContent();
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setMessage("Loading...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.show();


        // set activity title
        getActivity().setTitle("Route Result");

        // load map
        mMapView = (MapView) view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        // init db
        dbRef = FirebaseDatabase.getInstance().getReference();

        // all data loading is being done when map get loaded
        mMapView.getMapAsync(this);
        return view;
    }

    private void hideContent() {
        txtStart.setVisibility(View.INVISIBLE);
        txtEnd.setVisibility(View.INVISIBLE);
        driverName.setVisibility(View.INVISIBLE);
    }

    private void showContent() {
        txtStart.setVisibility(View.VISIBLE);
        txtEnd.setVisibility(View.VISIBLE);
        driverName.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // set traffic enabled
        googleMap.setTrafficEnabled(true);
        // get drive id
        driveID = getArguments().getString("driveID");
        //dbRef.child("drives").child(drive.driverID).child("meas").orderByKey();
        dbRef.child("drives").child(driveID).addListenerForSingleValueEvent(loadData);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    // update map when new meas added
    final ChildEventListener updateMap = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
            // getting current measurement
            Measurement currMeasurment = dataSnapshot.getValue(Measurement.class);
            LatLng newPoint = new LatLng(currMeasurment.latitude, currMeasurment.longitude);
            // adding to map
            String title = "Current Speed:" + currMeasurment.speed;
            String snippet = "Time Taken: " + Drives.dateFormat.format(currMeasurment.timeStamp);
            // add marker
            googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));
            // add to route
            List<LatLng> points = path.getPoints();
            points.add(newPoint);
            //add to path
            path.setPoints(points);
            // include in map for finish
            mapBuilder.include(newPoint);
            // set camera position to track latest
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 18));
            // set gauge
            speedometer.speedTo((currMeasurment.rpm/9000)*100, 1000);
            if (currMeasurment.rpm < 3000){
                rating.setText("Great");
            }
            else if (currMeasurment.rpm >= 3000 && currMeasurment.rpm < 6000) {
                rating.setText("Good");
            }
            else {
                rating.setText("Bad");
            }
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

    // load data initially
    ValueEventListener loadData = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            drive = dataSnapshot.getValue(Drives.class);
            // TODO: need to sort by time if not using indexes
            txtStart.setText("Start Time: " +  drive.getStartTime());
            if (drive.ongoing) {
                txtEnd.setText("Drive Is Active");
                txtEnd.setTextColor(Color.parseColor("#4CAF50"));
            }
            else {
                txtEnd.setText("End Time: " + drive.getEndTime());
            }
            // get driver data and it will call supervisor data
            dbRef.child("users").child(drive.driverID).child("username").addListenerForSingleValueEvent(loadDriverData);
            // load points to map
            pathOptions = new PolylineOptions().width(15).color(Color.RED);
            path = googleMap.addPolyline(pathOptions);
            // set map zoom
            mapBuilder = new LatLngBounds.Builder();
            // add points to map
            String title;
            String snippet;
            // used to update polyline
            LatLng newPoint;
            List<LatLng> points =  new ArrayList<>();
            // go over all of the measurements
            int entryNumber = 0;
            int size = drive.meas.entrySet().size();
            // sort keys to traverse in insert order
            Object[] keys = drive.meas.keySet().toArray();
            Arrays.sort(keys);
            for (Object key : keys)
            {
                if (entryNumber == 0) {
                    title = "Start Point";
                    snippet = "Current Speed: " + drive.meas.get(key).speed;
                }
                else if (entryNumber == size - 1 && size > 1 && drive.ongoing == false){
                    title = "Finish Point";
                    snippet = "Current Speed: " + drive.meas.get(key).speed;
                }
                else {
                    title = "Current Speed:" + drive.meas.get(key).speed;
                    snippet = "Time Taken: " + Drives.dateFormat.format(drive.meas.get(key).timeStamp);
                }
                if (entryNumber == size - 1) {
                    speedometer.speedTo((drive.meas.get(key).rpm/9000)*100, 1000);
                    if (drive.meas.get(key).rpm < 3000){
                        rating.setText("Great");
                    }
                    else if (drive.meas.get(key).rpm >= 3000 && drive.meas.get(key).rpm < 6000) {
                        rating.setText("Good");
                    }
                    else {
                        rating.setText("Bad");
                    }
                }
                // add to map
                newPoint = new LatLng(drive.meas.get(key).latitude, drive.meas.get(key).longitude);
                googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));
                // add to polyline
                points.add(newPoint);
                // include in map
                mapBuilder.include(newPoint);

                if (drive.ongoing == false) {
                    // set zoom to contain all path points
                    LatLngBounds bounds = mapBuilder.build();
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                    googleMap.animateCamera(cu);
                }
                // move to last marker
                else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() -1), 18));
                    // listener to new points
                    dbRef.child("drives").child(driveID).child("meas").addChildEventListener(updateMap);
                    // listener for finish event
                    dbRef.child("drives").child(driveID).child("ongoing").addValueEventListener(finishListener);
                }
                entryNumber++;
            }
            path.setPoints(points);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "failed loading drive", databaseError.toException());
        }
    };

    final ValueEventListener finishListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            boolean ongoing = dataSnapshot.getValue(Boolean.class);
            if (ongoing == false) {
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Drive Has Finished", Toast.LENGTH_LONG);
                toast.show();
                // set zoom to contain all path points
                LatLngBounds bounds = mapBuilder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                googleMap.animateCamera(cu);
                // update text

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            // ...
        }
    };

    // load driver data
    final ValueEventListener loadDriverData = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            driver = dataSnapshot.getValue(String.class);
            driverName.setText("Driver: " +  driver);
            // get supervisor data
            // finish
            // show data
            showContent();
            // hide progress bar
            mProgressDlg.dismiss();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "failed loading drive", databaseError.toException());
        }
    };
}
