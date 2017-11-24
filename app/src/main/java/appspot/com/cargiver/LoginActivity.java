package appspot.com.cargiver;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    // Request codes
    private static final int RC_SIGN_IN = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void onClickLogin(View v)
    {
        List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // Add user to db if it does not exist

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
                        //  User doesn't exist so add to db and continue
                        else {
                            User newUser = new User(user.getDisplayName(), user.getEmail(), User.UNKNOWN_TYPE);
                            dbRef.child("users").child(user.getUid()).setValue(newUser);
                            // redirect to choose activity
                            startActivity(new Intent(getBaseContext(), DriverOrSuperActivity.class));
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
                // Sign in failed
                if (response == null) {
                    Log.w(TAG, "Sign in failed - " + response.toString());
                    Toast toast = Toast.makeText(getApplicationContext(), "Error occurred please try later", Toast.LENGTH_SHORT);
                    toast.show();
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Error - No Internet connection", Toast.LENGTH_SHORT);
                    toast.show();
                }

                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.w(TAG, "Sign in failed - " + response.toString());
                    Toast toast = Toast.makeText(getApplicationContext(), "Error occurred please try later", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
    }
}
