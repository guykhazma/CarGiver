package appspot.com.cargiver;

import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MT on 27/17/2017.
 */

public class RouteResultFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    DatabaseReference dbRef;

    // data loaded from db
    String driveID;
    Drives drive; // will save current drive
    User driver;
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
    // distance objects
    int lastColorSeen = 0;
    LatLng lastPoint;
    Location temp;
    Location temp2;

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
        // init distance objects
        temp = new Location(LocationManager.GPS_PROVIDER);
        temp2 = new Location(LocationManager.GPS_PROVIDER);
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
        // get drive id
        driveID = getArguments().getString("driveID");
        dbRef.child("drives").child(driveID).addListenerForSingleValueEvent(loadData);
    }

    @Override
    public void onResume(){
        super.onResume();
        mMapView.onResume();
        // deselect all items
        NavigationView navigationView;
        // avoid view issues
        try {
            navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
            if (navigationView == null) {
                navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
            }
            navigationView.getMenu().getItem(0).setChecked(false);
            navigationView.getMenu().getItem(1).setChecked(false);
            navigationView.getMenu().getItem(2).setChecked(false);
            navigationView.getMenu().getItem(3).setChecked(false);
        }
        catch (Exception ex) {

        }
    }

    // update map when new meas added
    final ChildEventListener updateMap = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
            // getting current measurement
            Measurement currMeasurment = dataSnapshot.getValue(Measurement.class);
            LatLng newPoint = new LatLng(currMeasurment.latitude, currMeasurment.longitude);
            lastPoint = newPoint;
            // adding to map
            String title = "Current Speed:" + currMeasurment.speed;
            String snippet = "Time Taken: " + Drives.dateFormat.format(currMeasurment.timeStamp);
            // add new marker remove last
            lastMarker.remove();
            lastMarker = googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            // add to route
            List<LatLng> points = path.getPoints();
            points.add(newPoint);
            // add to current path
            path.setPoints(points);
            // include in map for finish
            mapBuilder.include(newPoint);
            // if we switched color switch poly line
            if (currMeasurment.color != lastColorSeen) {
                // clear points for next path
                points.clear();
                // green
                if (currMeasurment.color == 0) {
                    pathOptions = new PolylineOptions().width(15).color(Color.GREEN);
                    path = googleMap.addPolyline(pathOptions);
                    // add new point
                    points.add(newPoint);
                    // add to current path
                    path.setPoints(points);
                }
                else if (currMeasurment.color == 1) {
                    pathOptions = new PolylineOptions().width(15).color(Color.YELLOW);
                    path = googleMap.addPolyline(pathOptions);
                    // add new point
                    points.add(newPoint);
                    // add to current path
                    path.setPoints(points);
                }
                else if (currMeasurment.color == 2) {
                    pathOptions = new PolylineOptions().width(15).color(Color.RED);
                    path = googleMap.addPolyline(pathOptions);
                    // add new point
                    points.add(newPoint);
                    // add to current path
                    path.setPoints(points);
                }
                // update last color seen
                lastColorSeen = currMeasurment.color;
            }
            // set camera position to track latest
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 15));

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
            dbRef.child("users").child(drive.driverID).addListenerForSingleValueEvent(loadDriverData);
            // load points to map
            pathOptions = new PolylineOptions().width(15).color(Color.GREEN);
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
            points.add(newPoint);
            mapBuilder.include(newPoint);
            lastPoint = newPoint;
            googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet));

            // create route
            for (int i=1; i < drive.meas.size(); i++)
            {
                // add to route
                newPoint = new LatLng(drive.meas.get(i).latitude, drive.meas.get(i).longitude);
                // update last point
                lastPoint = newPoint;
                // add to polyline
                points.add(newPoint);
                // include in map
                mapBuilder.include(newPoint);
                // add marker if there was a change in color
                if (i < drive.meas.size() && drive.meas.get(i).color != lastColorSeen) {
                    // write latest path
                    path.setPoints(points);
                    // clear points for next path
                    points.clear();
                    // green
                    if (drive.meas.get(i).color == 0) {
                        pathOptions = new PolylineOptions().width(15).color(Color.GREEN);
                        path = googleMap.addPolyline(pathOptions);
                        // add new point
                        points.add(newPoint);
                    }
                    else if (drive.meas.get(i).color == 1) {
                        pathOptions = new PolylineOptions().width(15).color(Color.YELLOW);
                        path = googleMap.addPolyline(pathOptions);
                        // add new point
                        points.add(newPoint);
                    }
                    else if (drive.meas.get(i).color == 2) {
                        pathOptions = new PolylineOptions().width(15).color(Color.RED);
                        path = googleMap.addPolyline(pathOptions);
                        // add new point
                        points.add(newPoint);
                    }
                    // update last color seen
                    lastColorSeen = drive.meas.get(i).color;
                }
            }

            // add finish marker or current marker
            if (drive.ongoing == false) {
                title = "Finish Point";
                snippet = "Time: " + Drives.dateFormat.format(drive.endTime());
                // stop gauge tremble
                speedometer.setWithTremble(false);
            }
            else {
                title = "Current Speed:" + drive.meas.get(drive.meas.size() - 1).speed;
                snippet = "Time Taken: " + Drives.dateFormat.format(drive.endTime());
            }
            newPoint = new LatLng(drive.meas.get(drive.meas.size() - 1).latitude, drive.meas.get(drive.meas.size() - 1).longitude);
            lastMarker = googleMap.addMarker(new MarkerOptions().position(newPoint).title(title).snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            // set grade
            speedometer.speedTo(drive.grade, 1000);
            String Text;
            switch (drive.GradeReason){
                case 1 :
                    Text = "driving at high speed";
                    break;
                case 2 :
                    Text = "rapid speed changes";
                    break;
                case 3 :
                    Text = "high speed and rapid speed changes";
                    break;
                default:
                    Text = "";
                    break;
            }
            if(drive.GradeReason==0 && drive.grade>=33 && drive.grade<66){
                Text = "Good";
            }
            if(drive.GradeReason==0 && drive.grade>=66) {
                Text = "Bad";
            }
            if (drive.grade < 33){
                rating.setText("Great");
                rating.setTextColor(Color.GREEN);
            }
            else if (drive.grade >= 33 && drive.grade < 66) {
                rating.setText(Text);
                rating.setTextColor(Color.parseColor("#FFFFBB33"));
            }
            else {
                rating.setText(Text);
                rating.setTextColor(Color.RED);
            }

            // set zoom to contain all path points
            if (drive.ongoing == false) {
                LatLngBounds bounds = mapBuilder.build();
                // try showing all points
                try {
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                    googleMap.animateCamera(cu);
                }
                catch (Exception ex) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() -1), 15));
                }
            }
            // move to last marker
            else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() -1), 15));
                // listener to new points
                dbRef.child("drives").child(driveID).child("meas").orderByChild("timeStamp").startAt(new Date().getTime()).addChildEventListener(updateMap);
                // listener for finish event
                dbRef.child("drives").child(driveID).child("ongoing").addValueEventListener(finishListener);
            }
            // set path off last points
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
                // set zoom to contain all path points
                LatLngBounds bounds = mapBuilder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 50);
                googleMap.animateCamera(cu);
                // update text
                txtEnd.setText("Drive Has Finished");
                txtEnd.setTextColor(Color.parseColor("#9E9E9E"));

                // set grade
                speedometer.speedTo(drive.grade, 1000);
                String Text;
                switch (drive.GradeReason){
                    case 1 :
                        Text = "driving at high speed";
                        break;
                    case 2 :
                        Text = "rapid speed changes";
                        break;
                    case 3 :
                        Text = "high speed and rapid speed changes";
                        break;
                    default:
                        Text = "";
                        break;
                }
                if(drive.GradeReason==0 && drive.grade>=33 && drive.grade<66){
                    Text = "Good";
                }
                if(drive.GradeReason==0 && drive.grade>=66) {
                    Text = "Bad";
                }
                if (drive.grade < 33){
                    rating.setText("Great");
                    rating.setTextColor(Color.GREEN);
                }
                else if (drive.grade >= 33 && drive.grade < 66) {
                    rating.setText(Text);
                    rating.setTextColor(Color.parseColor("#FFFFBB33"));
                }
                else {
                    rating.setText(Text);
                    rating.setTextColor(Color.RED);
                }
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
            driver = dataSnapshot.getValue(User.class);
            driverName.setText("Driver: " + driver.getUsername());
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
        mMapView.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
