package appspot.com.cargiver;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Guy on 11/23/2017.
 */


public class StartActivity extends AppCompatActivity {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*---------------------Login Listener-------------------------------------*/
        FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // keep data fresh
                    DatabaseReference drives = FirebaseDatabase.getInstance().getReference("drives");
                    drives.keepSynced(true);
                    DatabaseReference drivers = FirebaseDatabase.getInstance().getReference("drivers");
                    drivers.keepSynced(true);
                    DatabaseReference supervisors = FirebaseDatabase.getInstance().getReference("supervisors");
                    supervisors.keepSynced(true);
                    DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
                    users.keepSynced(true);
                    DatabaseReference regtokens = FirebaseDatabase.getInstance().getReference("regTokens");
                    regtokens.keepSynced(true);
                    // user is already logged make sure it has type in db
                    final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
                    dbRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // if user exists check type
                            if (dataSnapshot.getValue() != null) {
                                // parse object to User class
                                User currentUser = dataSnapshot.getValue(User.class);
                                // user is driver
                                if (currentUser.type == User.DRIVER) {
                                    // Load driver activity
                                    startActivity(new Intent(getBaseContext(), MainDriverActivity.class));
                                    finish();
                                }
                                else if (currentUser.type == User.SUPERVISOR) {
                                    // Load supervisor activity
                                    startActivity(new Intent(getBaseContext(), MainSuperActivity.class));
                                    finish();
                                }
                                // User type is unknown meaning he still didn't choose
                                else {
                                    // redirect to choose activity
                                    startActivity(new Intent(getBaseContext(), DriverOrSuperActivity.class));
                                    finish();
                                }
                            }
                            //  User doesn't exist redirect to Login Activity
                            else {
                                startActivity(new Intent(getBaseContext(), LoginActivity.class));
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "DB failed - " + databaseError.getMessage());
                            Toast toast = Toast.makeText(getApplicationContext(), "Error occurred please try later", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });
                } else {
                    // user is not logged redirecting to login activity
                    startActivity(new Intent(getBaseContext(), LoginActivity.class));
                    finish();
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }
}
