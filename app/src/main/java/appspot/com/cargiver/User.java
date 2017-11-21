package appspot.com.cargiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing a user
 */

public class User {
    String username;
    String email;
    List<Car> carsOwned; // a list of cars the user owns
    List<User> watchers; // list of users the can watch the cars the user own
    List<Integer> drivesWatched; // watched drives id's associated with current user
    List<Integer> selfDrives; // self drives id's associated with current user

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.carsOwned = new ArrayList<>();
        this.watchers = new ArrayList<>();
        this.drivesWatched =  new ArrayList<>();
        this.selfDrives =  new ArrayList<>();
    }

    public void addOwnCar(String number, String model) {
        this.carsOwned.add(new Car(number,model));
    }

}
