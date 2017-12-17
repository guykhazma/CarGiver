package appspot.com.cargiver;

import android.app.Application;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by GK on 12/17/2017.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        // keep drives fresh
        DatabaseReference drives = FirebaseDatabase.getInstance().getReference("drives");
        drives.keepSynced(true);
    }
}
