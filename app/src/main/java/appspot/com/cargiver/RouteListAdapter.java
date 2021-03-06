package appspot.com.cargiver;

import android.widget.ArrayAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.text.*;

/**
 * Created by MT on 27/11/2017.
 */

public class RouteListAdapter extends ArrayAdapter {
    //to reference the Activity
    private final Activity context;
    //to store the list of routes
    private final String[] RoutesNameArray;
    private final List<Drives> RoutesList;
    public final List<String> RoutesIdList;


    public RouteListAdapter(Activity context, String[] nameArrayParam, List<Drives> DrivesList, List<String> DrivesIdList){

        super(context,R.layout.route_name , nameArrayParam);
        this.context=context;
        this.RoutesNameArray = nameArrayParam;
        this.RoutesList = DrivesList;
        this.RoutesIdList = DrivesIdList;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.route_name, null,true);

        //this code gets references to objects in the route_name.xml file
        TextView RouteDriverField = (TextView) rowView.findViewById(R.id.route_driver);
        TextView RouteDateField = (TextView) rowView.findViewById(R.id.route_date);

        //michaeltah - print the date in human readable way:
        SimpleDateFormat ft = new SimpleDateFormat ("dd.MM.yyyy HH:mm");

        //this code sets the values of the objects to values from the arrays
        StringBuffer DriverField = new StringBuffer("Driver: ");
        DriverField.append(RoutesNameArray[position]);
        StringBuffer DateField = new StringBuffer("Date: ");
        if(RoutesList.get(position).ongoing){ //if this drive still ongoing
            RouteDateField.setText("ongoing drive");
            RouteDateField.setTextColor(Color.GREEN);
        }else{
            DateField.append(Drives.dateFormat.format(RoutesList.get(position).startTime()));
            RouteDateField.setText(DateField);
        }
        RouteDriverField.setText(DriverField);

        return rowView;

    };

}
