package appspot.com.cargiver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class DriverOrSuperActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_or_super);
    }
    protected void DriverMain(View v)
    {
        startActivity(new Intent(getBaseContext(), MainActivity.class));
    }
    protected void SuperMain(View v)
    {
        startActivity(new Intent(getBaseContext(), MainSuperActivity.class));
    }
}
