package appspot.com.cargiver;

import java.util.List;

/**
 * Created by GK on 11/24/2017.
 */

public class Driver {

    List<String> supervisorsIDs; // the key of the supervisors
    List<Supervisor> pendingSupervisorIDs; // the key of the supervisors pending
    List<String> drivesIDs; // keys of drives related to the supervisor (key as generated by firebase)
    float grade;
    float TotalKm;

    public Driver() {
        // Default constructor required for calls to DataSnapshot.getValue(Driver.class)
    }
}