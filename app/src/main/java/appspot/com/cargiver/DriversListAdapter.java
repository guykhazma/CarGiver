package appspot.com.cargiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Guy on 12/9/2017.
 */
public class DriversListAdapter extends ArrayAdapter {
    //to reference the Activity
    private final Activity context;
    //to store the list of drivers
    private final List<String> Drivers;


    public DriversListAdapter(Activity context, List<String>Drivers){

        super(context,R.layout.manage_driver_listitem , Drivers);
        this.context=context;
        this.Drivers=Drivers;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.manage_driver_listitem, null,true);
        //this code gets references to objects in the route_name.xml file
        TextView DriverField = (TextView) rowView.findViewById(R.id.textDriver);
        DriverField.setText(Drivers.get(position));
        return rowView;

    };

}
