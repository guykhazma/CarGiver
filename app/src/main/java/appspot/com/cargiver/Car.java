package appspot.com.cargiver;

/**
 * Created by GK on 11/21/2017.
 */

public class Car {
    String number;
    String model;

    public Car() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Car(String number, String model) {
        this.model = model;
        this.number = number;
    }
}
