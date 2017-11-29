package appspot.com.cargiver;
//package com.mkyong.android;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
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
/**
 * Created by Guybb96 on 11/27/2017.
 */

public class ManageSupervisorsFragment extends Fragment {
    private EditText editTxt;
    private Button btn;
    //private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> listViewAdapter;
    private ArrayList<String> names;
    public ManageSupervisorsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        names=new ArrayList<String>();
        //String[] names = { "Apple","it","Jackfruit", "Mango", "Olive", "Pear", "Sugar-apple" };

        // Get reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = database.getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid(); // current user id
        // Get list of authorized supervisor IDs.
        final List<String> supervisorIDs = new ArrayList<String>();
        //dbRef.child("drivers").child(uid).child("supervisorIDs")
        dbRef.child("drivers").child(uid).child("supervisorsIDs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    supervisorIDs.add(child.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.driver_manage_supervisors_fragment, container, false);
        final Button button = (Button)view.findViewById(R.id.addBtn);
        // set as active in drawer
        // set menu as selected on startup
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view_driver);
        navigationView.getMenu().getItem(2).setChecked(true);

        getActivity().setTitle("Manage Supervisors");

        ListView listView=(ListView)view.findViewById(R.id.listItem);
        listViewAdapter=new ArrayAdapter<String>(
            getActivity(),android.R.layout.simple_list_item_1,
            supervisorIDs
        );
        listView.setAdapter(listViewAdapter);
        //handle click on buttom
        button.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                                //Intent myIntent = new Intent(view.getContext(), agones.class);
                                //startActivityForResult(myIntent, 0);

                        names.add("clicked");
                        //new code
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Add a Supervisor");
                        // I'm using fragment here so I'm using getView() to provide ViewGroup
                        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
                        View viewInflated = LayoutInflater.from(v.getContext()).inflate(R.layout.text_input_supervisor, (ViewGroup) getView(), false);
                        // Set up the input
                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        builder.setView(viewInflated);

                        // Set up the buttons
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                String m_Text = input.getText().toString();
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

                        //end new code
                        /*
                        //set the list
                        listViewAdapter.notifyDataSetChanged();
                        AlertDialog alertDialog = new AlertDialog.Builder(v.getContext()).create(); //Read Update
                        // Set up the input
                        final EditText input = (EditText) view.findViewById(R.id.input);
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        alertDialog.setView(view);
                        alertDialog.setTitle("hi");
                        alertDialog.setMessage("this is my app");

                        alertDialog.setButton("Continue..", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // here you can add functions
                                    }
                                });

                        alertDialog.show();  //<-- See This!
                    }

                        });*/

        return view;
    }
    public void AddSupervisor(View v)
    {
      //pop dialog to enter email of supervisor and if exists put this supervisor
    }
}
