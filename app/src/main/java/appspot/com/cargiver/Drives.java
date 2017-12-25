package appspot.com.cargiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    // date format
    static SimpleDateFormat dateFormat = new SimpleDateFormat ("dd.MM.yyyy HH:mm");


    ArrayList<Measurement> meas;
    Boolean ongoing;
    String driverID;
    float grade;
    long StartTimeStamp; // !!!NEGATIVE!!! time stamp

    public Drives() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Date startTime() {
        return new Date(meas.get(0).timeStamp);
    }

    public Date endTime() {
        return new Date(meas.get(meas.size() - 1).timeStamp);
    }
}
