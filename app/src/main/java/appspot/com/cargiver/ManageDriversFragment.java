package appspot.com.cargiver;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
/**
 * Created by Stav on 27/11/2017.
 */

import android.app.Fragment;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Speedometer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageDriversFragment extends Fragment {
    private ProgressDialog mProgressDlg;
    String DriveID;
    boolean ChosenDrive;
    Button btnWatchPervDrive;
    AlertDialog.Builder builder1;
    AlertDialog alert11;
    List<String> driverIDs;
    DriversListAdapter MyAdapter;
    DatabaseReference dbRef;
    List<String> driverMails;
    HashMap<String , Integer> mailPositionMap;

    public ManageDriversFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dbRef.removeEventListener(driverChanged);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.manage_drivers_fragment, container, false);

        // Get reference
        dbRef = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = currentUser.getUid(); // current user id
        // Get list of authorized driver IDs.


        //define listview
        driverMails = new ArrayList<String>();
        driverIDs = new ArrayList<String>();
        mailPositionMap = new HashMap<>();
        MyAdapter = new DriversListAdapter(getActivity(), driverMails);
        //default adapter
        final ListView listView = (ListView) view.findViewById(R.id.listItem);
        listView.setAdapter(MyAdapter);
        // getting authorized drivers id's
        // show loading
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setMessage("Loading...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.show();
        dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    driverIDs.add(child.getKey());
                }

                // Convert IDs to emails
                dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            // Check if this user correlates to a supervisor of current user
                            if (driverIDs.contains(child.getKey())) {
                                String mail=child.getValue(User.class).getEmail();
                                driverMails.add(mail);
                                mailPositionMap.put(child.getKey(),driverMails.size() - 1);
                            }
                        }
                        //if he doesn't have drivers he can see
                        if(driverMails.size()==0) {
                            AlertDialog.Builder NoDrivers = new AlertDialog.Builder(getActivity());
                            NoDrivers.setTitle("no drivers");
                            NoDrivers.setMessage("you currently have no drivers to watch");
                            NoDrivers.setNegativeButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                        }
                                    });
                            NoDrivers.create();
                            NoDrivers.show();
                        }

                        // register listener for changes
                        dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").addChildEventListener(driverChanged);
                        MyAdapter.notifyDataSetChanged();
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // hide progress bar
                                mProgressDlg.dismiss();
                            }
                        }, 500);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //set on item on list view clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                builder1 = new AlertDialog.Builder(view.getContext());
                final View viewThis=inflater.inflate(R.layout.driver_details,null);

                builder1.setView(viewThis);
                builder1.setCancelable(true);

                //this is the button that shows the recent drive if exist
                btnWatchPervDrive = (Button)viewThis.findViewById(R.id.recent_drive);
                btnWatchPervDrive.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ChosenDrive==false){
                            return;
                        }else{
                            Fragment ShowRouteRes = new RouteResultFragment();
                            // set parameters to fragment
                            Bundle bundle = new Bundle();
                            bundle.putString("driveID", DriveID);
                            ShowRouteRes.setArguments(bundle);
                            getFragmentManager().beginTransaction().replace(R.id.fragment_container_super, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();
                            alert11.dismiss();
                        }
                    }
                });

                // get grades and totalKM of drivers
                dbRef.child("drivers").child(driverIDs.get(position)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        /////////////driver details////////////////////////
                        Driver drv = dataSnapshot.getValue(Driver.class);
                        //set total kms
                        TextView textViewKms = (TextView)viewThis.findViewById(R.id.DriverTotalKm);
                        StringBuffer TotalKmText = new StringBuffer("Total KM driven: ");
                        TotalKmText.append(Integer.toString((int)drv.totalKm));
                        TotalKmText.append(" km");
                        textViewKms.setText(TotalKmText);

                        //set drivers grade:
                        TextView textViewGrade = (TextView)viewThis.findViewById(R.id.DriverGrade);
                        StringBuffer myGradetext = new StringBuffer("Driver's summary: ");
                        myGradetext.append(ConvertGradeToText(drv.grade, drv.TotalMeas, drv.TotalHighSpeed, drv.TotalSpeedChanges));

                        textViewGrade.setText(myGradetext);

                        //set the speedometer
                        Speedometer speedometer;
                        speedometer = (Speedometer)  viewThis.findViewById(R.id.speedViewMD);
                        speedometer.speedTo(drv.grade, 1000);
                        speedometer.setWithTremble(false);

                        dbRef.child("users").child(driverIDs.get(position)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                User curr = dataSnapshot.getValue(User.class);
                                //////////////////set driver name//////////////////////
                                TextView textViewDriverName = (TextView)viewThis.findViewById(R.id.DriverName);
                                StringBuffer MyDriverName = new StringBuffer("Name: ");
                                MyDriverName.append(curr.getUsername());
                                textViewDriverName.setText(MyDriverName);
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });

                        //////////////////set route details////////////////
                        dbRef.child("drives").orderByChild("StartTimeStamp").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                ChosenDrive = false;
                                for (DataSnapshot Child: dataSnapshot.getChildren()) {
                                    if(ChosenDrive) {break;}
                                    Drives CurrDrive = Child.getValue(Drives.class);
                                    if(CurrDrive.driverID.equals(driverIDs.get(position))){
                                        DriveID = Child.getKey(); //this is the drive ID
                                        ChosenDrive = true;
                                        btnWatchPervDrive.setText("Watch recent drive");
                                        // hide progress bar
                                        mProgressDlg.dismiss();
                                    }
                                    mProgressDlg.dismiss();
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                builder1.setNegativeButton("back",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {dialog.cancel();}
                        });
                builder1.setPositiveButton(
                        "Delete Driver",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String driverID = driverIDs.get(position);
                                dbRef.child("drivers").child(driverID).child("supervisorsIDs").child(uid).removeValue();
                                dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").child(driverID).removeValue();
                                driverMails.remove(position);
                                driverIDs.remove(position);
                                MyAdapter.notifyDataSetChanged();
                                dialog.dismiss();
                                dbRef.child("regTokens").child(driverID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        String regToken = dataSnapshot.getValue(String.class);
                                        NotificationService.sendNotification("Deleted you from their drivers list!", regToken);
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                        });
                alert11 = builder1.create();
                alert11.show();

                // show loading
                mProgressDlg = new ProgressDialog(getActivity());
                mProgressDlg.setMessage("Loading...");
                mProgressDlg.setCancelable(false);
                mProgressDlg.show();
            }
        });
        return view;
    }

    final ChildEventListener driverChanged = new ChildEventListener() {
        public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
            final String key = dataSnapshot.getKey();
            // add only if it is new and not first time
            if (!driverIDs.contains(key)) {
                // add id to list
                driverIDs.add(dataSnapshot.getKey());
                // Convert IDs to emails
                dbRef.child("users").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User usr = dataSnapshot.getValue(User.class);
                        driverMails.add(usr.getEmail());
                        mailPositionMap.put(key, driverMails.size() - 1);
                        MyAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String removedID = dataSnapshot.getKey();
            int index = driverIDs.indexOf(removedID);
            // if in list remove
            if (index != - 1) {
                driverIDs.remove(removedID);
                driverMails.remove((int) mailPositionMap.get(removedID));
                mailPositionMap.remove(removedID);
            }
            MyAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    public static String ConvertGradeToText(float Grade, int TotalNumOfMeas,int TotalHighSpeed, int TotalSpeedChanges){
        if (Grade == 0){ //no drives yes
            return "Doesn't have drives yet";
        }
        if (Grade < 33){
            return "Great!";
        }
        if(TotalNumOfMeas==0) { //backward compatibale
            if (Grade >= 33 && Grade < 66) {
                return "Good";
            } else {
                return "Bad";
            }
        }
        if (((double)TotalHighSpeed/TotalNumOfMeas)>0.1 && ((double) TotalSpeedChanges/TotalNumOfMeas)>0.1){
            return "high speed and rapid speed changes";
        }
        if (((double) TotalSpeedChanges/TotalNumOfMeas)>0.1){
            return "rapid speed changes";
        }
        if (((double) TotalHighSpeed/TotalNumOfMeas)>0.1){
            return "driving at high speed";
        }
        //if we don't have enough information about the driving
        if (Grade >= 33 && Grade < 66) {
            return "Good";
        } else {
            return "Bad";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(2).setChecked(true);
        getActivity().setTitle("Manage Drivers");
    }

}