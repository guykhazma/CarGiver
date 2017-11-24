package appspot.com.cargiver;

import android.location.Location;

import java.util.Calendar;
import java.util.Date;
/**
 * Created by GK on 11/24/2017.
 */

// Represent measurement during drive

public class Measurement {
    //TODO: check what is the required type for time stamp and location
    Date timeStamp; // the time stamp of the measurement
    int speed; // current vehicle speed
    Location loc;

    public Measurement() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Measurement(int speed, Location loc) {
        this.speed = speed;
        this.loc = loc;
        this.timeStamp = Calendar.getInstance().getTime();
    }
}