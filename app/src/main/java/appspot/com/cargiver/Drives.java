package appspot.com.cargiver;

import java.util.List;

/**
 * Created by GK on 11/24/2017.
 */

// Drives are identified by id created by firebase when pushing

public class Drives {

    List<Measurement> meas; // measurements associated with this drive
    String timeStarted;
    String timeEnded;
    String currentDriverID;
    String supervisorID;

    public Drives() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }
}
