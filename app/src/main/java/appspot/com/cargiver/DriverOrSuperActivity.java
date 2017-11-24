package appspot.com.cargiver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverOrSuperActivity extends AppCompatActivity {

    FirebaseUser user;
    DatabaseReference dbRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_or_super);

        // init db and login variable
        user = FirebaseAuth.getInstance().getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    // Redirect to driver main on click
    protected void DriverMain(View v)
    {
        // update db set to driver type
        dbRef.child("users").child(user.getUid()).child("type").setValue(User.DRIVER);
        startActivity(new Intent(getBaseContext(), MainDriverActivity.class));
        finish();
    }

    // Redirect to super main on click
    protected void SuperMain(View v)
    {
        // update db set to super type
        dbRef.child("users").child(user.getUid()).child("type").setValue(User.SUPERVISOR);
        startActivity(new Intent(getBaseContext(), MainSuperActivity.class));
        finish();
    }
}
