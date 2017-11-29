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

/**
 * Created by MT on 27/11/2017.
 */

public class RouteListAdapter extends ArrayAdapter {
    //to reference the Activity
    private final Activity context;
    //to store the list of routes
    private final String[] RoutesNameArray;
    public final List<Drives> RoutesList;

    public RouteListAdapter(Activity context, String[] nameArrayParam, List<Drives> DrivesList){

        super(context,R.layout.route_name , nameArrayParam);
        this.context=context;
        this.RoutesNameArray = nameArrayParam;
        this.RoutesList = DrivesList;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.route_name, null,true);

        //this code gets references to objects in the route_name.xml file
        TextView RouteDriverField = (TextView) rowView.findViewById(R.id.route_driver);
        TextView RouteDateField = (TextView) rowView.findViewById(R.id.route_date);

        //this code sets the values of the objects to values from the arrays
        RouteDriverField.setText(RoutesNameArray[position]);
        RouteDateField.setText(RoutesList.get(position).getStartTime());

        return rowView;

    };

}
