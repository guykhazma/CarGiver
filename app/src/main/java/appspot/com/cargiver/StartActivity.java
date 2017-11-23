package appspot.com.cargiver;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

/**
 * Created by Guy on 11/23/2017.
 */

public class StartActivity extends AppCompatActivity {
    /*----------------------------------Login----------------------------------------*/
    private FirebaseAuth.AuthStateListener authListener;
    private String username;
    private String photoUrl;
    private String emailAddress;
    /*------------------ Firebase DB-----------------------*/
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*---------------------Login Listener-------------------------------------*/
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // TODO: update nav_header_main
                    username = user.getDisplayName();
                    photoUrl = user.getPhotoUrl().toString();
                    emailAddress = user.getEmail();
                    User newUser = new User(username, emailAddress);
                    // add user to db TODO: add it only if it is a new registration
                    mDatabase.child("users").child(user.getUid()).setValue(newUser);


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
