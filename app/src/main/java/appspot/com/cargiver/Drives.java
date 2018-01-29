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
    public static SimpleDateFormat dateFormat = new SimpleDateFormat ("dd.MM.yyyy HH:mm");


    public ArrayList<Measurement> meas;
    public Boolean ongoing;
    public String driverID;
    public float grade;
    public float totalKm;
    public long StartTimeStamp; // !!!NEGATIVE!!! time stamp
    public int GradeReason; //0-ok, 1-high speed, 2-speed changes, 3-both
    public int TotalMeas; //total measurments for this driver
    public int TotalHighSpeed;
    public int TotalSpeedChanges;

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
