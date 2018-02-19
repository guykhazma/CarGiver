package appspot.com.cargiver;
//package com.mkyong.android;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.app.Fragment;
import android.widget.Button;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Guybb96 on 11/27/2017.
 */

public class ManageSupervisorsFragment extends Fragment {
    private EditText editTxt;
    private ArrayAdapter<String> listViewAdapter;
    DatabaseReference dbRef;
    List<String> supervisorIDs;
    List<String> supervisorMails;
    HashMap<String , Integer> mailPositionMap;
    public SupervisorsListViewAdapter superAdapter;
    private ProgressDialog mProgressDlg;
    public ManageSupervisorsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        supervisorIDs = new ArrayList<String>();
        supervisorMails = new ArrayList<String>();
        mailPositionMap = new HashMap<>();
        // Get reference
        dbRef = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = currentUser.getUid(); // current user id
        //define listview
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.driver_manage_supervisors_fragment, container, false);
        ListView listView=(ListView)view.findViewById(R.id.listItem);
        listViewAdapter=new ArrayAdapter<String>(
                getActivity(),R.layout.manage_supervisor_listitem,R.id.textSupervisor,
                supervisorMails
        );
        //listView.setAdapter(listViewAdapter);
        superAdapter=new SupervisorsListViewAdapter(getActivity(),supervisorMails);
        superAdapter.supervisorIDs = supervisorIDs;
        listView.setAdapter(superAdapter);
        mProgressDlg = new ProgressDialog(getActivity());
        mProgressDlg.setMessage("Loading...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.show();
        // Get list of authorized supervisor_item IDs.
        dbRef.child("drivers").child(uid).child("supervisorsIDs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    supervisorIDs.add(child.getKey());
                }

                // Convert IDs to emails
                dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            // Check if this user correlates to a supervisor of current user
                            if(supervisorIDs.contains(child.getKey())){
                                //add mail to list
                                String mail=child.getValue(User.class).getEmail();
                                supervisorMails.add(mail);
                                mailPositionMap.put(child.getKey(),supervisorMails.size() - 1);
                            }
                        }
                        // register listener for changes
                        dbRef.child("drivers").child(uid).child("supervisorsIDs").addChildEventListener(supervisorChanged);
                        superAdapter.notifyDataSetChanged();

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

        // set as active in drawer
        // set menu as selected on startup
        final Button button = (Button) view.findViewById(R.id.addBtn);
        //code for adding supervisor. no need for now
        //handle click on bottom
        button.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        //new code
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Add a Supervisor");
                        View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.text_input_supervisor, (ViewGroup) getView(), false);
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
                                                if (user.email.equals(finalEmail) && user.type == User.SUPERVISOR) {
                                                    //add supervisor to his list
                                                    String mail2=m_Text;
                                                    supervisorMails.add(mail2);
                                                    mailPositionMap.put(child.getKey(),supervisorMails.size() - 1);
                                                    supervisorIDs.add(child.getKey());
                                                    //add the supervisor to the drivers list
                                                    dbRef.child("drivers").child(uid).child("supervisorsIDs").child(child.getKey()).setValue(true);
                                                    //add the driver to supervisors
                                                    dbRef.child("supervisors").child(child.getKey()).child("authorizedDriverIDs").child(uid).setValue(true);
                                                    //listViewAdapter.notifyDataSetChanged();
                                                    superAdapter.notifyDataSetChanged();
                                                    // Send notification to supervisor
                                                    String supervisorId = child.getKey();
                                                    dbRef.child("regTokens").child(supervisorId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            String regToken = dataSnapshot.getValue(String.class);
                                                            NotificationService.sendNotification("Added you as a supervisor!", regToken);
                                                        }
                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {

                                                        }
                                                    });
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

        // listen to deletes
        // Get list of authorized supervisor_item IDs.

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(2).setChecked(true);
        getActivity().setTitle("Manage Supervisors");
    }

    final ChildEventListener supervisorChanged = new ChildEventListener() {
        public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
            final String key = dataSnapshot.getKey();
            // add only if it is new and not first time
            if (!supervisorIDs.contains(key)) {
                // add id to list
                supervisorIDs.add(dataSnapshot.getKey());
                // Convert IDs to emails
                dbRef.child("users").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User usr = dataSnapshot.getValue(User.class);
                        supervisorMails.add(usr.getEmail());
                        mailPositionMap.put(key, supervisorMails.size() - 1);
                        superAdapter.notifyDataSetChanged();
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
            int index = supervisorIDs.indexOf(removedID);
            // if in list remove
            if (index != - 1) {
                // if it is a remote remove then the size is equal and we remove here, otherwise for local remove the adapter handles it.
                if (supervisorMails.size() == supervisorIDs.size()) {
                    supervisorMails.remove((int) mailPositionMap.get(removedID));
                }
                supervisorIDs.remove(removedID);
                // remove email only if it's a supervisor deletion meaning mail still in list
                mailPositionMap.remove(removedID);
            }
            superAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public void onDestroyView() {
        super.onDestroyView();
        dbRef.removeEventListener(supervisorChanged);
    }
}
