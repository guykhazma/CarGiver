package appspot.com.cargiver;
//package com.mkyong.android;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
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
    public ManageSupervisorsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final List<String> supervisorIDs = new ArrayList<String>();
        final List<String> supervisorMails = new ArrayList<String>();
        // Get reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference dbRef = database.getReference();
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
        final SupervisorsListViewAdapter superAdapter=new SupervisorsListViewAdapter(getActivity(),supervisorMails);
        listView.setAdapter(superAdapter);
            // Get list of authorized supervisor_item IDs.
            dbRef.child("drivers").child(uid).child("supervisorsIDs").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    for (DataSnapshot child : children) {
                        supervisorIDs.add(child.getKey());
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
                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    // Check if this user correlates to a supervisor of current user
                    if(supervisorIDs.contains(child.getKey())){
                        supervisorMails.add(child.getValue(User.class).getEmail());
                        //listViewAdapter.notifyDataSetChanged();
                        superAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(2).setChecked(true);
        getActivity().setTitle("Manage Supervisors");
        final Button button = (Button) view.findViewById(R.id.addBtn);
        //code for adding supervisor. no need for now
        //handle click on buttom
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
                                    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for (DataSnapshot child : dataSnapshot.child("users").getChildren()) {
                                                User user = child.getValue(User.class);
                                                if (user.email.equals(finalEmail) && user.type == User.SUPERVISOR) {
                                                    //add supervisor to his list
                                                    supervisorMails.add(m_Text);
                                                    //listViewAdapter.notifyDataSetChanged();
                                                    superAdapter.notifyDataSetChanged();
                                                    // Send notification to supervisor
                                                    String supervisorId = child.getKey();
                                                    String regToken = dataSnapshot.child("regTokens").child(supervisorId).getValue(String.class);
                                                    NotificationService.sendNotification("Added you as a supervisor!", regToken);
                                                    //add the supervisor to the drivers list
                                                    dbRef.child("drivers").child(uid).child("supervisorsIDs").child(child.getKey()).setValue(true);
                                                    //add the driver to supervisors
                                                    dbRef.child("supervisors").child(child.getKey()).child("authorizedDriverIDs").child(uid).setValue(true);

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
        //set on item on list view clicked
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
//                AlertDialog.Builder builder1 = new AlertDialog.Builder(view.getContext());
//                builder1.setMessage("Do you want to delete this Supervisor?");
//                builder1.setCancelable(true);
//                builder1.setNegativeButton("No",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.cancel();
//                            }
//                        });
//                builder1.setPositiveButton(
//                        "Yes",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.dismiss();
//                                dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(DataSnapshot dataSnapshot) {
//                                        String superID="";
//                                        String superMail=supervisorMails.get(position);
//                                        supervisorMails.remove(position);
//                                        listViewAdapter.notifyDataSetChanged();
//                                        for (DataSnapshot child : dataSnapshot.child("users").getChildren()) {
//                                            if (child.getValue(User.class).email.equals(superMail)) {
//                                                superID=child.getKey();
//                                                dbRef.child("drivers").child(uid).child("supervisorsIDs").child(superID).removeValue();
//                                                dbRef.child("supervisors").child(superID).child("authorizedDriverIDs").child(uid).removeValue();
//                                                // Send notification
//                                                String regToken = dataSnapshot.child("regTokens").child(superID).getValue(String.class);
//                                                NotificationService.sendNotification("Deleted you from their supervisor list!", regToken);
//                                                break;
//                                            }
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(DatabaseError databaseError) {
//
//                                    }
//                                });
//                            }
//                        });
//                final AlertDialog alert11 = builder1.create();
//                alert11.show();
//            }
//        });
        return view;
    }

}
