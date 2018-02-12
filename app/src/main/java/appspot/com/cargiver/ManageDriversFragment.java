package appspot.com.cargiver;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Patterns;
import 	java.util.regex.Pattern;
/**
 * Created by Stav on 27/11/2017.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Speedometer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ManageDriversFragment extends Fragment {
    private ArrayAdapter<String> listViewAdapter;
    private ArrayList<String> Drivernames;
    //for popup window
    private Button test;
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private ProgressDialog mProgressDlg;
    String DriveID;
    boolean ChosenDrive;
    Button btnWatchPervDrive;
    AlertDialog.Builder builder1;
    AlertDialog alert11;

    public ManageDriversFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.manage_drivers_fragment, container, false);
        View elemview = inflater.inflate(R.layout.manage_driver_listitem, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(2).setChecked(true);

        getActivity().setTitle("Manage Drivers");
        // Get reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference dbRef = database.getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = currentUser.getUid(); // current user id
        // Get list of authorized driver IDs.
        final List<String> driverIDs = new ArrayList<String>();
        //grades of drivers
        final float[] driverGrades ;
        final float[] driverKms ;
        int driversCnt;
        //define listview
        final List<String> driverMails = new ArrayList<String>();
        //final Button button = (Button) view.findViewById(R.id.addBtn);
        Drivernames = new ArrayList<String>();
        //listViewAdapter=new ArrayAdapter<String>(
        //        getActivity(),R.layout.manage_driver_listitem,R.id.textDriver,
        //         driverMails
        // );
        //my adapter
        final DriversListAdapter MyAdapter = new DriversListAdapter(getActivity(), driverMails);
        //default adapter
        final ListView listView = (ListView) view.findViewById(R.id.listItem);
        listView.setAdapter(MyAdapter);
        dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    driverIDs.add(child.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        // Convert IDs to emails
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    // Check if this user correlates to a supervisor of current user
                    if (driverIDs.contains(child.getKey())) {
                        driverMails.add(child.getValue(User.class).getEmail());
                        MyAdapter.notifyDataSetChanged();
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        driversCnt=driverMails.size();
        driverGrades=new float[driversCnt];
        driverKms=new float[driversCnt];
        //set on item on list view clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                builder1 = new AlertDialog.Builder(view.getContext());
                //builder1.setMessage("Driver's details");
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
                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        /////////////driver details////////////////////////
                        DataSnapshot Drivers = dataSnapshot.child("drivers");
                        for (DataSnapshot child : Drivers.getChildren()) {
                            // Check if this user correlates to a supervisor of current user
                            if (driverIDs.contains(child.getKey()) && child.getKey().equals(driverIDs.get(position))) {
                                //set total kms
                                TextView textViewKms = (TextView)viewThis.findViewById(R.id.DriverTotalKm);
                                StringBuffer TotalKmText = new StringBuffer("Total KM driven: ");
                                TotalKmText.append(Integer.toString((int)child.getValue(Driver.class).totalKm));
                                TotalKmText.append(" km");
                                textViewKms.setText(TotalKmText);

                                //set drivers grade:
                                TextView textViewGrade = (TextView)viewThis.findViewById(R.id.DriverGrade);
                                StringBuffer myGradetext = new StringBuffer("Driver's summary: ");
                                myGradetext.append(ConvertGradeToText(child.getValue(Driver.class).grade, child.getValue(Driver.class).TotalMeas,
                                        child.getValue(Driver.class).TotalHighSpeed, child.getValue(Driver.class).TotalSpeedChanges));

                                textViewGrade.setText(myGradetext);

                                //set the speedometer
                                Speedometer speedometer;
                                speedometer = (Speedometer)  viewThis.findViewById(R.id.speedViewMD);
                                speedometer.speedTo(child.getValue(Driver.class).grade, 1000);
                                speedometer.setWithTremble(false);

                            }
                        }

                        //////////////////set driver name//////////////////////
                        TextView textViewDriverName = (TextView)viewThis.findViewById(R.id.DriverName);
                        StringBuffer MyDriverName = new StringBuffer("Name: ");
                        MyDriverName.append(dataSnapshot.child("users").child(driverIDs.get(position)).getValue(User.class).getUsername());
                        textViewDriverName.setText(MyDriverName);

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
                                dialog.dismiss();
                                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        String driverID = "";
                                        String driverMail = driverMails.get(position);
                                        driverMails.remove(position);
                                        MyAdapter.notifyDataSetChanged();
                                        for (DataSnapshot child : dataSnapshot.child("users").getChildren()) {
                                            if (child.getValue(User.class).email.equals(driverMail)) {
                                                driverID = child.getKey();
                                                dbRef.child("drivers").child(driverID).child("supervisorsIDs").child(uid).removeValue();
                                                dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").child(driverID).removeValue();
                                                // Send notification to Driver that he was deleted
                                                String regToken = dataSnapshot.child("regTokens").child(driverID).getValue(String.class);
                                                NotificationService.sendNotification("Deleted you from their driver list!", regToken);
                                                break;
                                            }
                                        }
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
////////////////////////////////////////////////////////////////////////////////////
        // Present list of authorized driver names.
        //handle click on buttom code for adding drivers
        /*
        button.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Add a Driver");
                        View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.text_input_driver, (ViewGroup) getView(), false);
                        // Set up the input
                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        // Specify the type of input expected;
                        builder.setView(viewInflated);
                        //builder alert not good mail
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(v.getContext());
                        builder1.setMessage("This is an invalid Email.");
                        builder1.setCancelable(true);
                        builder1.setNegativeButton(
                                "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        final AlertDialog alert11 = builder1.create();
                        // Set up the buttons
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                final String m_Text = input.getText().toString();
                                //check that mail is valid and belongs to some Driver
                                Pattern pattern = Patterns.EMAIL_ADDRESS;
                                final String finalEmail = new String(m_Text);
                                final List<String> exists = new ArrayList<String>();
                                //check tha is valid mail form
                                if(pattern.matcher(m_Text).matches()==false  )
                                {
                                    //alert not valid
                                    alert11.show();
                                }
                                //check if this Email exists
                                else {
                                    dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                                User user = child.getValue(User.class);
                                                if (user.email.equals(finalEmail) && user.type == User.DRIVER) {
                                                    //add driver to list of authorized drivers in this supervisor
                                                    dbRef.child("supervisors").child(uid).child("authorizedDriverIDs").child(child.getKey()).setValue(true);
                                                    //add the supervisor to the drivers list
                                                    dbRef.child("drivers").child(child.getKey()).child("supervisorsIDs").child(uid).setValue(true);
                                                    driverMails.add(m_Text);
                                                    listViewAdapter.notifyDataSetChanged();
                                                    return;
                                                }
                                            }
                                            //no such mail of Driver
                                            alert11.show();
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show(); }
                });
        //handle deleteing a driver
//        rmvButton.setOnClickListener(
//                new View.OnClickListener() {
//                    public void onClick(View v) {
//                        AlertDialog.Builder builder2 = new AlertDialog.Builder(v.getContext());
//                        builder2.setTitle("Delete a Driver");
//                        builder2.setMessage("Are you sure you want to delete this driver?");
//                        // Set up the buttons
//                        builder2.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                                //if he pressed ok delete this driver
//                            }
//                        });
//                        builder2.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                        builder2.show(); }
//                });
*/


        return view;
    }
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

}