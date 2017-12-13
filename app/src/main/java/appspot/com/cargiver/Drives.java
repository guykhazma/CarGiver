package appspot.com.cargiver;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Created by GK on 11/24/2017.
 */

// Drives are identified by id created by firebase when pushing

public class Drives {

    //todo stavsh add comnstructor
    // date format
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Map<String, Measurement> meas;
    Boolean ongoing;
    String driverID;
    String supervisorID;

    public Drives() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    // data is ordered so latest is first
    public String getStartTime() {
        return dateFormat.format(meas.get(meas.keySet().toArray()[meas.keySet().size()-1]).timeStamp);

    }
    public String getDriverID() {
        return this.driverID;
    }
    public String getSupervisorID() {
        return this.supervisorID;
    }

    public String getEndTime() {
        if (ongoing == true) {
            return "Drive is active";
        }
        return dateFormat.format(meas.get(meas.keySet().toArray()[0]).timeStamp);
    }
}
