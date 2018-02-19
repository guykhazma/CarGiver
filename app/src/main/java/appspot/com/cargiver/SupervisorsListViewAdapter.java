package appspot.com.cargiver;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * Created by Guy on 1/13/2018.
 */

public class SupervisorsListViewAdapter extends ArrayAdapter<String> {
    //to reference the Activity
    private final Activity context;
    public List<String> supervisorIDs;

    public SupervisorsListViewAdapter(Activity context,List<String> objects){

        super(context,R.layout.manage_driver_listitem,objects);
        this.context=context;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = context.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.manage_supervisor_listitem, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //set data to views
        holder.name.setText(getItem(position));

        holder.btnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(getItem(position),position);
            }
        });

        return convertView;
    }
    public void showAlertDialog(final String str,final int position) {
        // Get reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference dbRef = database.getReference();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = currentUser.getUid(); // current user id
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Confirm");
        builder.setCancelable(true);
        builder.setMessage("Are you sure?");
        builder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String supID = supervisorIDs.get(position);
                        dbRef.child("drivers").child(uid).child("supervisorsIDs").child(supID).removeValue();
                        dbRef.child("supervisors").child(supID).child("authorizedDriverIDs").child(uid).removeValue();
                        // send notification
                        dbRef.child("regTokens").child(supID).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String regToken = dataSnapshot.getValue(String.class);
                                NotificationService.sendNotification("Deleted you from their supervisor list!", regToken);
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        dialog.dismiss();
                    }
                });

        builder.show();
    }

    private class ViewHolder {
        private TextView name;
        private ImageView btnDel;

        public ViewHolder(View v) {
            name = (TextView) v.findViewById(R.id.textSupervisor);
            btnDel = (ImageView) v.findViewById(R.id.btn_dlt);
        }
    }
}
