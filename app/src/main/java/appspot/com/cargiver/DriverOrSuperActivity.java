package appspot.com.cargiver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

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
    public void DriverMain(View v)
    {
        AlertDialog.Builder DriverHlp = new AlertDialog.Builder(this);
        DriverHlp.setTitle("I Am Driver");
        DriverHlp.setMessage("Driver Activity:\nyou will connect to the obd before start driving, and all of your supervisors will be able to see your routes details");
        DriverHlp.setPositiveButton("AGREE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // update db set to driver type, and add to drivers
                        dbRef.child("users").child(user.getUid()).child("type").setValue(User.DRIVER);
                        //set default vlues for new users
                        dbRef.child("drivers").child(user.getUid()).child("TotalHighSpeed").setValue(0);
                        dbRef.child("drivers").child(user.getUid()).child("TotalMeas").setValue(0);
                        dbRef.child("drivers").child(user.getUid()).child("TotalSpeedChanges").setValue(0);
                        dbRef.child("drivers").child(user.getUid()).child("grade").setValue(0);
                        dbRef.child("drivers").child(user.getUid()).child("totalKm").setValue(0);
                        startActivity(new Intent(getBaseContext(), MainDriverActivity.class));
                        finish();
                    }
                });
        DriverHlp.setNegativeButton("BACK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setContentView(R.layout.activity_driver_or_super);
                    }
                });
        DriverHlp.create();
        DriverHlp.show();

    }

    // Redirect to super main on click
    public void SuperMain(View v)
    {
        AlertDialog.Builder DriverHlp = new AlertDialog.Builder(this);
        DriverHlp.setTitle("I Am Supervisor");
        DriverHlp.setMessage("Supervisor Activity:\nyou can define your drivers, and see their routes details");
        DriverHlp.setPositiveButton("AGREE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // update db set to super type
                        dbRef.child("users").child(user.getUid()).child("type").setValue(User.SUPERVISOR);
                        startActivity(new Intent(getBaseContext(), MainSuperActivity.class));
                        finish();
                    }
                });
        DriverHlp.setNegativeButton("BACK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setContentView(R.layout.activity_driver_or_super);
                    }
                });
        DriverHlp.create();
        DriverHlp.show();

    }

    public void DriverHelp(View v) {//michaeltah
        AlertDialog.Builder DriverHlp = new AlertDialog.Builder(this);
        DriverHlp.setTitle("I Am Driver");
        DriverHlp.setMessage("Driver Activity:\nyou will connect to the obd before start driving, and all of your supervisors will be able to see your routes details");
        DriverHlp.setPositiveButton("GOT IT",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setContentView(R.layout.activity_driver_or_super);
                    }
                });
        DriverHlp.create();
        DriverHlp.show();
    }
    public void SuperHelp(View v) {//michaeltah
        AlertDialog.Builder DriverHlp = new AlertDialog.Builder(this);
        DriverHlp.setTitle("I Am Supervisor");
        DriverHlp.setMessage("Supervisor Activity:\nyou can define your drivers, and see their routes details");
        DriverHlp.setPositiveButton("GOT IT",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setContentView(R.layout.activity_driver_or_super);
                    }
                });
        DriverHlp.create();
        DriverHlp.show();
    }
}
