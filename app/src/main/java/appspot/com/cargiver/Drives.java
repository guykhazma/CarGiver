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
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    ArrayList<Measurement> meas;
    Boolean ongoing;
    String driverID;
    float grade;



    public Drives() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Date startTime() {
        return meas.get(0).timeStamp;
    }

    public Date endTime() {
        return meas.get(meas.size() - 1).timeStamp;
    }
}
