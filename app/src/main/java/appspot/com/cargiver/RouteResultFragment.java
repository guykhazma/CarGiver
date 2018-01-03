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
import java.util.Date;
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
import com.google.android.gms.maps.model.Marker;
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
    Marker lastMarker;
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
            // add new marker remove last
            lastMarker.remove();
            lastMarker = googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));
            // add to route
            List<LatLng> points = path.getPoints();
            points.add(newPoint);
            //add to path
            path.setPoints(points);
            // include in map for finish
            mapBuilder.include(newPoint);
            // set camera position to track latest
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 18));

            dataSnapshot.getRef().getParent().getParent().child("grade").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // set gauge
                    float DriveGrade = dataSnapshot.getValue(float.class);

                    speedometer.speedTo(DriveGrade, 1000);

                    if (DriveGrade < 33){
                        rating.setText("Great");
                        rating.setTextColor(Color.GREEN);
                    }
                    else if (DriveGrade >= 33 && DriveGrade < 66) {
                        rating.setText("Good");
                        rating.setTextColor(Color.parseColor("#FFFFBB33"));
                    }
                    else {
                        rating.setText("Bad");
                        rating.setTextColor(Color.RED);
                    }

                }
                public void onCancelled(DatabaseError databaseError) {
                }
            });

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

            // set texts
            txtStart.setText("Start Time: " +  Drives.dateFormat.format(drive.startTime()));
            if (drive.ongoing) {
                txtEnd.setText("Drive Is Active");
                txtEnd.setTextColor(Color.parseColor("#4CAF50"));
            }
            else {
                txtEnd.setText("End Time: " + Drives.dateFormat.format(drive.endTime()));
            }

            // add start marker
            title = "Start Point";
            snippet = "Time: " +  Drives.dateFormat.format(drive.startTime());
            newPoint = new LatLng(drive.meas.get(0).latitude, drive.meas.get(0).longitude);
            googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));

            // create route
            for (int i=0; i < drive.meas.size(); i++)
            {
                // add to route
                newPoint = new LatLng(drive.meas.get(i).latitude, drive.meas.get(i).longitude);
                // add to polyline
                points.add(newPoint);
                // include in map
                mapBuilder.include(newPoint);;
            }

            // add finish marker or current marker
            if (drive.ongoing == false) {
                title = "Finish Point";
                snippet = "Time: " + Drives.dateFormat.format(drive.endTime());
                // stop gauge tremble
                speedometer.setWithTremble(false);
                // don't show traffic status for finished drive
                googleMap.setTrafficEnabled(false);
            }
            else {
                title = "Current Speed:" + drive.meas.get(drive.meas.size() - 1).speed;
                snippet = "Time Taken: " + Drives.dateFormat.format(drive.endTime());
            }
            newPoint = new LatLng(drive.meas.get(drive.meas.size() - 1).latitude, drive.meas.get(drive.meas.size() - 1).longitude);
            lastMarker = googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));

            // set grade
            speedometer.speedTo(drive.grade, 1000);
            if (drive.grade < 33){
                rating.setText("Great");
                rating.setTextColor(Color.GREEN);
            }
            else if (drive.grade >= 33 && drive.grade < 66) {
                rating.setText("Good");
                rating.setTextColor(Color.parseColor("#FFFFBB33"));
            }
            else {
                rating.setText("Bad");
                rating.setTextColor(Color.RED);
            }

            // set zoom to contain all path points
            if (drive.ongoing == false) {

                LatLngBounds bounds = mapBuilder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                googleMap.animateCamera(cu);
            }
            // move to last marker
            else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() -1), 18));
                // listener to new points
                dbRef.child("drives").child(driveID).child("meas").orderByChild("timeStamp").startAt(new Date().getTime()).addChildEventListener(updateMap);
                // listener for finish event
                dbRef.child("drives").child(driveID).child("ongoing").addValueEventListener(finishListener);
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
                if (getActivity() != null) {
                    Toast toast = Toast.makeText(getActivity(), "Drive Has Finished", Toast.LENGTH_SHORT);
                    toast.show();
                }
                // stop gauge tremble
                speedometer.setWithTremble(false);
                // don't show traffic status for finished drive
                googleMap.setTrafficEnabled(false);
                // set zoom to contain all path points
                LatLngBounds bounds = mapBuilder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                googleMap.animateCamera(cu);
                // update text
                txtEnd.setText("Drive Has Finished");
                txtEnd.setTextColor(Color.parseColor("#9E9E9E"));
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

    @Override
    // unregister listeners
    public void onDestroyView() {
        super.onDestroyView();
        dbRef.removeEventListener(loadData);
        dbRef.removeEventListener(finishListener);
        dbRef.removeEventListener(updateMap);
    }
}
