package appspot.com.cargiver;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    /*----------------------------------Login----------------------------------------*/
    private FirebaseAuth.AuthStateListener authListener;
    private String username;
    private String photoUrl;
    private String emailAddress;

    /*---------------------------------Fragment Related---------------------------------*/
    private FragmentManager fragmentManager;

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



        /*------------------init DB----------------------*/
        dbRef = FirebaseDatabase.getInstance().getReference();

        /*------------------------- Bluetooth Init-------------------------------*/
        // Listen to bluetooth events
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        // progress bar for bluetooth scan
        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Cancel Discovery
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        mNewDevicesArrayList = new ArrayList<BluetoothDevice>(); // will save newly found devices

        // Bluetooth action button
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        // If the adapter is null, then Bluetooth is not supported so update bluetooth icon
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Device has no bluetooth");
            has_bluetooth = false;
            // TODO: check if theme should be  context.getTheme() as in https://stackoverflow.com/questions/33140706/android-dynamically-change-fabfloating-action-button-icon-from-code
            fab.setImageDrawable(getResources().getDrawable(R.drawable.stat_sys_data_bluetooth_disabled, null));
            //TODO: set background to grey to indicate unclickable
        }

        /*-------------------- Main Fragment initialization --------------------------------------------*/
        // Set  fragment manager
        fragmentManager = getFragmentManager(); // For AppCompat use getSupportFragmentManager

        // if we are just starting
        if (savedInstanceState == null) {
            // Create Main Fragment
            MainDriverFragment main = new MainDriverFragment();
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            main.setArguments(getIntent().getExtras());
            // load default activity
            fragmentManager.beginTransaction().replace(R.id.fragment_container_driver,main).commit();
        }
    }

    /**
     * Broadcast receiver reacts to bluetooth events
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Handle Bluetooth actions
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_OFF) {
                    // TODO: switch icon to red
                    Log.w(TAG, "Bluetooth Disabled");
                    CharSequence text = "Failed Enabling Bluetooth";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mProgressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Discovery Finished");
                mProgressDlg.dismiss();
                // Open device_list_fragment
                Fragment scanFragmenet = new DeviceListFragment();
                // add the scanned devices
                Bundle bundle=new Bundle();
                bundle.putParcelableArrayList("device.list", mNewDevicesArrayList);
                scanFragmenet.setArguments(bundle);
                if (scanFragmenet != null) {
                    fragmentManager.beginTransaction().replace(R.id.fragment_container_driver, scanFragmenet).commit();
                }
            }
            // when discovery finds a device
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayList.add(device);
                }
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
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                bluetooth_enabled = true;
                Log.w(TAG, "Bluetooth Enabled");
                // Bluetooth is now enabled, so set up scan
                mBluetoothAdapter.startDiscovery();
            }
            else {
                Log.w(TAG, "Bluetooth Enabled");
                Context context = getApplicationContext();
                CharSequence text = "Failed Enabling Bluetooth";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_sign_out) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            // user is now signed out
                            startActivity(new Intent(MainDriverActivity.this, LoginActivity.class));
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
            // Bluetooth is now enabled, so set up scan
            else {

                mBluetoothAdapter.startDiscovery();
                mProgressDlg.show();
            }
        }
    }
}
