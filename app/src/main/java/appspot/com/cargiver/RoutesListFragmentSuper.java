package appspot.com.cargiver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

/**
 * Created by MT on 27/11/2017.
 */

public class RoutesListFragmentSuper extends Fragment {
    public ListView RouteslistView;
    public DatabaseReference TheRoutesDB;
    public RoutesListFragmentSuper(){};
    public int index = 0;
    String[] nameArray;
    int SecondIndex;
    // progress dialog
    private ProgressDialog mProgressDlg;
    String uid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.routes_list, container, false);
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_super);
        navigationView.getMenu().getItem(1).setChecked(true);
        getActivity().setTitle("Routes List");

        // show loading
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setMessage("Loading...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.show();

        RouteslistView = (ListView) view.findViewById(R.id.routes_list_view);

        //take last 20 routes from db. if less take less...

        //michaeltah - take information from db
        TheRoutesDB = FirebaseDatabase.getInstance().getReference();
        //TheRoutesDB.child("drives");
        //todo michaetlah - see if we need drives list for another use
        final List<Drives> DrivesList = new ArrayList<Drives>(); //will keep the drives data
        final List<String> DrivesIdList = new ArrayList<String>(); //will keep the drives id
        final List<String> MyUsers = new ArrayList<String>(); //will keep the users i can see

        //1. get my user id
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser.getUid(); // current user id

        //2. get my drivers
        TheRoutesDB.child("supervisors").child(uid).child("authorizedDriverIDs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot Child : dataSnapshot.getChildren()) {
                    MyUsers.add(Child.getKey());
                }

                //3. get all the drives that this supervisor can see
                TheRoutesDB.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int NumOfRoutes = 0; //we want only 20 routes
                        DataSnapshot Drives = dataSnapshot.child("drives");
                        for (DataSnapshot Child: Drives.getChildren()) {
                            if (NumOfRoutes >= 20){break;}
                            Drives CurrDrive = Child.getValue(Drives.class);
                            if(MyUsers.contains(CurrDrive.driverID)){
                                DrivesList.add(CurrDrive);
                                DrivesIdList.add(Child.getKey());
                                NumOfRoutes++;
                            }
                        }

                        DataSnapshot DBUsers = dataSnapshot.child("users");
                        nameArray = new String[DrivesList.size()];

                        //4. get the usernames for the drives
                        for(index=0; index<DrivesList.size(); index++) {
                            nameArray[index] = DBUsers.child(DrivesList.get(index).driverID).child("username").getValue(String.class);
                        }

                        //5. set the adapter and update the relevant fields
                        if(DrivesList.size()==0) {//if he doesn't have drives he can see
                            AlertDialog.Builder NoDrives = new AlertDialog.Builder(getActivity());
                            NoDrives.setTitle("no previous routes");
                            NoDrives.setMessage("you can see routes by adding new drivers in \"Manage Drivers\"");
                            NoDrives.setPositiveButton("GOT IT",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                        }
                                    });
                            NoDrives.create();
                            NoDrives.show();
                        }else {
                            RouteListAdapter MyAmazingAdapter = new RouteListAdapter(getActivity(), nameArray, DrivesList, DrivesIdList);
                            RouteslistView.setAdapter(MyAmazingAdapter);
                        }
                        // hide progress bar
                        mProgressDlg.dismiss();

                        RouteslistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
                                RouteListAdapter btnAdapter = (RouteListAdapter)parent.getAdapter();
                                String did = btnAdapter.RoutesIdList.get(position);
                                Fragment ShowRouteRes = new RouteResultFragment();
                                // set parameters to fragment
                                Bundle bundle = new Bundle();
                                bundle.putString("driveID", did);
//                        bundle.putString("driveID", "-KzyH3elX37eUbJZNd6l");
                                ShowRouteRes.setArguments(bundle);
                                getFragmentManager().beginTransaction().replace(R.id.fragment_container_super, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                } );


            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });



        return view;
    }



}
