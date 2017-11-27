package appspot.com.cargiver;

import java.util.List;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by GK on 11/24/2017.
 */

// Drives are identified by id created by firebase when pushing

public class Drives {

    List<Measurement> meas; // measurements associated with this drive
    Boolean ongoing;
    String driverID;
    String supervisorID;

    public Drives() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Date getStartTime() {
        return meas.get(0).timeStamp;
    }

    public Date getEndTime() {
        if (ongoing == true) {
            return null;
        }
        return meas.get(meas.size() - 1).timeStamp;
    }
}
