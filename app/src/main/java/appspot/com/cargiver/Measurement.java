package appspot.com.cargiver;

import android.location.Location;
import android.support.annotation.Keep;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;
/**
 * Created by GK on 11/24/2017.
 */

// Represent measurement during drive

public class Measurement {
    long timeStamp; // the time stamp of the measurement
    int speed; // current vehicle speed
    int rpm;
    double latitude;
    double longitude;
    int color; //0=green, 1=orange, 2=red

    @Keep
    public Measurement() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Measurement(int speed, double latitude, double longitude, int rpm, int color) {
        this.speed = speed;
        this.rpm = rpm;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = Calendar.getInstance().getTime().getTime();
        this.color = color;
    }

    public Date date() {
        return new Date(timeStamp);
    }

}
