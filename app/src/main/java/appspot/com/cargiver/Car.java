package appspot.com.cargiver;

/**
 * Created by GK on 11/21/2017.
 */

public class Car {
    String carName;
    String carNumber;
    String carColor;

    public Car() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Car(String carName, String carNumber, String carColor) {
        this.carName = carName;
        this.carNumber = carNumber;
        this.carColor = carColor;
    }
}
