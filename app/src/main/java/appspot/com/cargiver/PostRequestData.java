package appspot.com.cargiver;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stav on 21/12/2017.
 */

public class PostRequestData {
    @SerializedName("data")
    @Expose
    private Data data;
    @SerializedName("registration_ids")
    @Expose
    private String[] registration_ids;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String[] getRegistration_ids() {
        return this.registration_ids;
    }

    // Set a single receiver
    public void setRegistration_ids(String to) {
        this.registration_ids = new String[1];
        this.registration_ids[0] = to;
    }

    // Set a list of receivers
    public void setRegistration_ids(List<String> to) {
        this.registration_ids = to.toArray(new String[to.size()]);
    }
}
