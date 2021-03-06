package appspot.com.cargiver;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stav on 29/11/2017.
 */

public class MainSuperFragment extends Fragment {
    public DatabaseReference TheRoutesDB;
    String uid;
    String DriveID;
    boolean ChosenDrive;
    private ProgressDialog mProgressDlg;
    Button btnOngoingDrive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.main_fragment_supervisor, container, false);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(0).setChecked(true);

        getActivity().setTitle("Supervisor Page");

        btnOngoingDrive = view.findViewById(R.id.super_ongoing_drive);
        Button btnRoutesList = view.findViewById(R.id.super_routes_drive);
        Button btnManageDrivers = view.findViewById(R.id.super_manage_drivers);

        //if we have ongoing drive then set the text for btnOngoingDrive:
        //michaeltah - take information from db
        final List<String> MyUsers = new ArrayList<String>(); //will keep the users i can see
        ChosenDrive=false;
        TheRoutesDB = FirebaseDatabase.getInstance().getReference();
        //1. get my user id
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser.getUid(); // current user id
        String usr = null;
        if (currentUser.getDisplayName()!=null && !currentUser.getDisplayName().equals("")) {
            StringBuffer MyUserName = new StringBuffer("Hello ");
            MyUserName.append(currentUser.getDisplayName());
            getActivity().setTitle(MyUserName);
        }
        else {
            // try getting data from provider
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (usr == null && userInfo.getDisplayName() != null) {
                    usr = userInfo.getDisplayName();
                }
            }
            if (usr != null && !usr.equals("")) {
                getActivity().setTitle("Hello " + usr);
            } else {
                getActivity().setTitle("Hello Supervisor");
            }
        }

        //2. get my drivers
        TheRoutesDB.child("supervisors").child(uid).child("authorizedDriverIDs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                 // show loading
                mProgressDlg = new ProgressDialog(getActivity());
                mProgressDlg.setMessage("Loading...");
                mProgressDlg.setCancelable(false);
                mProgressDlg.show();

                for (DataSnapshot Child : dataSnapshot.getChildren()) {
                    MyUsers.add(Child.getKey()); //all my drivers
                }
                 //3. get all the drives that this supervisor can see
                TheRoutesDB.child("drives").orderByChild("ongoing").equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot Child : dataSnapshot.getChildren()) {
                            Drives CurrDrive = Child.getValue(Drives.class);
                            if (MyUsers.contains(CurrDrive.driverID)) { //if i'm the supervisor
                                if (CurrDrive.ongoing == true && ChosenDrive == false) { //we choose this drive
                                    DriveID = Child.getKey(); //this is the drive ID
                                    ChosenDrive = true;
                                    //print the name of the driver:
                                    TheRoutesDB.child("users").child(CurrDrive.driverID).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            User curr = dataSnapshot.getValue(User.class);
                                            //////////////////set driver name//////////////////////
                                            StringBuffer MyDriverName = new StringBuffer("Watch ongoing drive\nDriver: ");
                                            MyDriverName.append(curr.getUsername());
                                            btnOngoingDrive.setText(MyDriverName);
                                            btnOngoingDrive.setTextSize(25);
                                            btnOngoingDrive.setBackground(getResources().getDrawable(R.drawable.greenbuttonshape));

                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {}
                                    });

                                }
                            }
                        }
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


        btnOngoingDrive.setOnClickListener( new View.OnClickListener() {
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
                }
            }
        });

        btnRoutesList.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pop back fragments till reaching menu
                getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                // Redirect to manage drives fragment
                // open list of routes
                Fragment RoutesListFragmentSuper = new RoutesListFragmentSuper();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container_super, RoutesListFragmentSuper, "Drives List");
                getActivity().setTitle("Routes List");
                // add to stack to allow return to menu on back press
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        btnManageDrivers.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // pop back fragments till reaching menu
                getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                // Redirect to manage drives fragment
                // open list of routes
                Fragment ManageDriversFragment = new ManageDriversFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container_super, ManageDriversFragment, "Manage Drivers");
                getActivity().setTitle("Manage Drivers");
                // add to stack to allow return to menu on back press
                transaction.addToBackStack(null);
                transaction.commit();
                // Redirect to manage drivers fragment
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(0).setChecked(true);
    }
}
