package appspot.com.cargiver;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class MainDriverActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainDriverActivity.class.getName(); // TAG for logging

    /*--------------------------Bluetooth-----------------------------------------------*/
    private final static int REQUEST_ENABLE_BT = 1; // for bluetooth request response code
    private static boolean bluetooth_enabled = false;
    private static boolean has_bluetooth = true;
    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ArrayList<BluetoothDevice> mNewDevicesArrayList; // Newly discovered devices
    private ProgressDialog mProgressDlg; // progress bar for search

    /*------------------ Firebase DB-----------------------*/
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure user is logged if not redirect to start activity to handle this
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getBaseContext(), StartActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main_driver);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_driver);
        setSupportActionBar(toolbar);

        // Drawer init and Navigation
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_driver);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_driver);
        navigationView.setNavigationItemSelectedListener(this);

        // Fill current user details
        View navHeaderView= navigationView.getHeaderView(0);
        TextView txt = (TextView) navHeaderView.findViewById(R.id.name);
        txt.setText(user.getDisplayName());
        TextView email = (TextView) navHeaderView.findViewById(R.id.email);
        email.setText(user.getEmail());
        // load image
        ImageView img = (ImageView) navHeaderView.findViewById(R.id.imageView);
        Picasso.with(getBaseContext()).load(user.getPhotoUrl()).into(img);

        /*------------------init DB----------------------*/
        dbRef = FirebaseDatabase.getInstance().getReference();

        /*------------------------- Bluetooth Init-------------------------------*/
        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        // Adapter changes
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // connection changes
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, filter);

        // Bluetooth action button
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        // If the adapter is null, then Bluetooth is not supported so update bluetooth icon
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Device has no bluetooth or bluetooth is disabled");
            fab.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_data_bluetooth_disabled, null));
            fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        }
        // display as connected if already connected
        if (BluetoothOBDService.status == BluetoothOBDService.STATE_CONNECTED) {
            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.stat_sys_data_bluetooth , null));
            fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        /*-------------------- Main Fragment initialization --------------------------------------------*/
        // if we are just starting
        if (savedInstanceState == null) {
            // Create Main Fragment
            MainDriverFragment main = new MainDriverFragment();
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            main.setArguments(getIntent().getExtras());
            // load default activity
            getFragmentManager().beginTransaction().replace(R.id.fragment_container_driver,main,"MAIN").commit();
        }
    }

    /**
     * Broadcast receiver reacts to bluetooth events
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            // Handle Bluetooth actions
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                Log.w(TAG, "State Change + " + state);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.w(TAG, "Bluetooth Disabled");
                    // Switch to inactive bluetooth
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_data_bluetooth_disabled, null));
                    fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth disconnected", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else if (state == BluetoothAdapter.STATE_ON) {
                    Log.w(TAG, "Bluetooth Enabled");
                    // Switch to inactive bluetooth
                    fab.setImageDrawable(getResources().getDrawable(android.R.drawable.stat_sys_data_bluetooth , null));
                    fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DD2C00")));
                    Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Enabled", Toast.LENGTH_SHORT);
                    toast.show();

                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.w(TAG, "Bluetooth Connection Lost");
                synchronized (BluetoothOBDService.class) {
                    BluetoothOBDService.dev = null;
                    BluetoothOBDService.status = BluetoothOBDService.STATE_DISCONNECTED;
                    BluetoothOBDService.sock = null;
                }
                // Switch to inactive bluetooth
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.stat_sys_data_bluetooth , null));
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DD2C00")));
                Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Connection Lost", Toast.LENGTH_SHORT);
                toast.show();
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))  {
                Log.w(TAG, "Bluetooth Connection Lost");
                // Switch to inactive bluetooth
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.stat_sys_data_bluetooth , null));
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                Toast toast = Toast.makeText(getApplicationContext(), "Connected successfully", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    /**
     * Reaction to activities according to activities codes
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check response to bluetooth enable
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.w(TAG, "Bluetooth Enabled");
                Fragment scanFragment = new DeviceListFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container_driver, scanFragment, "BT_scan");
                // add to stack to allow return to menu on back press
                transaction.addToBackStack(null);
                transaction.commit();
                // remove menu selection from drawer
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_driver);
                navigationView.getMenu().getItem(0).setChecked(false);
            }
            else {
                Log.w(TAG, "Bluetooth Disabled");
                // Switch color to red
                fab.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_data_bluetooth_disabled, null));
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    // Menu on back pressed - auto generated
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_driver);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_driver, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main_driver) {
            // pop back fragments till reaching menu
            getFragmentManager().popBackStack(0,FragmentManager.POP_BACK_STACK_INCLUSIVE);

        } else if (id == R.id.nav_manage_supervisors_driver) {
            // pop back fragments till reaching menu
            getFragmentManager().popBackStack(0,FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // Redirect to manage supervisors fragment

        } else if (id == R.id.nav_drives_driver) {
            // pop back fragments till reaching menu
            getFragmentManager().popBackStack(0,FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // Redirect to manage drives fragment
        } else if (id == R.id.nav_sign_out_driver) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            // user is now signed out
                            startActivity(new Intent(getBaseContext(), LoginActivity.class));
                            finish();
                        }
                    });
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_driver);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    // Bluetooth FAB on click
    public void bluetoothFBOnClick(View view){
        // check for bluetooth support
        if (!has_bluetooth) {
            Snackbar.make(view, "Oops...It seems your phone doesn't have Bluetooth", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
        else  {
            // Bluetooth exist check if enabled
            if (!mBluetoothAdapter.isEnabled()) {
                // ask user to enable bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            // Bluetooth is now enabled, so go to scan page
            else {
                // open scan page
                Fragment scanFragment = new DeviceListFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container_driver, scanFragment, "BT_scan");
                // add to stack to allow return to menu on back press
                transaction.addToBackStack(null);
                transaction.commit();
                // remove menu selection from drawer
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view_driver);
                navigationView.getMenu().getItem(0).setChecked(false);
            }
        }
    }
}
