package appspot.com.cargiver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
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
    public int index = 0;
    String[] nameArray;
    // progress dialog
    private ProgressDialog mProgressDlg;
    String MyUserName; //this is the driver's username
    String uid; //this is the driver's Uid

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.routes_list, container, false);
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
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
        final List<Drives> DrivesList = new ArrayList<Drives>(); //will keep the drives data
        final List<String> DrivesIdList = new ArrayList<String>(); //will keep the drives id

        //1. get my user id
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        uid = currentUser.getUid(); // current user id
        //2. get my username
        TheRoutesDB.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MyUserName = dataSnapshot.getValue(User.class).getUsername();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        } );

        //3. get my drives list
        TheRoutesDB.child("drives").orderByChild("driverID").equalTo(uid).limitToFirst(20).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int NumOfRoutes = 0;
                for (DataSnapshot Child: dataSnapshot.getChildren()) {
                    if (NumOfRoutes >= 20){break;}
                        Drives CurrDrive = Child.getValue(Drives.class);
                        if(CurrDrive.driverID.equals(uid)) {
                            DrivesList.add(0,CurrDrive);
                            DrivesIdList.add(0,Child.getKey());
                            NumOfRoutes++;
                        }
                }
                nameArray = new String[DrivesList.size()];
                for(int i=0; i<DrivesList.size(); i++){
                    nameArray[i]= MyUserName;
                }

                if(DrivesList.size()==0){ //doesn't have old drives
                    AlertDialog.Builder NoDrives = new AlertDialog.Builder(getActivity());
                    NoDrives.setTitle("no previous routes");
                    NoDrives.setMessage("you will be able to see previous routes after you will record them with this app");
                    NoDrives.setPositiveButton("GOT IT",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                }
                            });
                    NoDrives.create();
                    NoDrives.show();
                }else{
                    RouteListAdapter MyAmazingAdapter = new RouteListAdapter(getActivity(), nameArray, DrivesList, DrivesIdList);
                    RouteslistView.setAdapter(MyAmazingAdapter);
                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // hide progress bar
                        mProgressDlg.dismiss();
                    }
                }, 500);

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
                        getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver, ShowRouteRes, ShowRouteRes.getClass().getSimpleName()).addToBackStack(null).commit();

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        } );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(1).setChecked(true);
        getActivity().setTitle("Routes List");
    }

}
