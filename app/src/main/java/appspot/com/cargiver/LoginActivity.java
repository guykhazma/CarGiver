package appspot.com.cargiver;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    // login variables
    private FirebaseAuth.AuthStateListener authListener;
    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    /*----------------------------------Login----------------------------------------*/
    private String username;
    private String photoUrl;
    private String emailAddress;

    /*------------------ Firebase DB-----------------------*/
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int flaglogin;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*------------------init DB----------------------*/
        mDatabase = FirebaseDatabase.getInstance().getReference();
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

                }
            }
        };

        FirebaseAuth.getInstance().addAuthStateListener(authListener);
        /*super.onCreate(savedInstanceState);*/
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
                mAuth = FirebaseAuth.getInstance();
                //TODO:
                //check if driver or supervisor
                // Successfully signed in move to main/supermain activity
                startActivity(new Intent(getBaseContext(), DriverOrSuperActivity.class));
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    //TODO:
                    //showSnackbar(R.string.sign_in_cancelled);
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    //TODO:
                    //showSnackbar(R.string.no_internet_connection);
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    //TODO:
                    //showSnackbar(R.string.unknown_error);
                    return;
                }
            }
            //TODO:
            //showSnackbar(R.string.unknown_sign_in_response);
        }
    }
}
