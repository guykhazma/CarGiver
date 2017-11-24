package appspot.com.cargiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing a user
 */



public class User {

    final static int UNKNOWN_TYPE = 0;
    final static int DRIVER = 1;
    final static int SUPERVISOR = 2;

    String username;
    String email;
    int type;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email, int userType) {
        this.username = username;
        this.email = email;
        this.type = UNKNOWN_TYPE;
    }
}
