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
    Date timeStamp; // the time stamp of the measurement
    float speed; // current vehicle speed
    float rpm;
    float latitude;
    float longitude;
    float GForce;

    @Keep
    public Measurement() {
        // Default constructor required for calls to DataSnapshot.getValue(Drives.class)
    }

    public Measurement(int speed, float latitude, float longitude, int rpm, float GForce) {
        this.speed = speed;
        this.rpm = rpm;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = Calendar.getInstance().getTime();
        this.GForce= GForce;
    }
}
