package appspot.com.cargiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing a user
 */

// User is identified in the DB by UID provided to the user by firebase

public class User {

    public final static int UNKNOWN_TYPE = 0;
    public final static int DRIVER = 1;
    public final static int SUPERVISOR = 2;

    public String username;
    public String email;
    public int type;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email, int userType) {
        this.username = username;
        this.email = email;
        this.type = UNKNOWN_TYPE;
    }

    public String getUsername(){
        if (this.username != null)
            return this.username;
        else
            return this.email;
    }
    public String getEmail(){
        return this.email;
    }
}
