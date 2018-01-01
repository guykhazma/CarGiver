package appspot.com.cargiver;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;



public class BluetoothOBDService extends Service implements SensorEventListener {
    // Accelerometer variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity

    private static final String TAG = "BluetoothOBDService";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private NotificationManager mNM;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int NOTIFICATION = R.string.OBDservice;

    // location
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    // Binder given to clients
    private final IBinder mBinder = new BluetoothOBDBinder();

    private List<String> regTokens;
    private String address;
    private String uid;
    private String driveKey;
    public  boolean stopped; // indicates whether failure caused the problem

    private DatabaseReference dbref;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    // Accelerometer method
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta; // todo stav: tweak calculation to detect crash

        // Send notification in case of crash
        if (mAccel > 11) {  // todo stav: tweak calculation to detect crash
            String msg = "Dangerous Driving!";
            NotificationService.sendNotification(msg, regTokens);
        }
    }

    @Override
    // Accelerometer method
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public class BluetoothOBDBinder extends Binder {
        public BluetoothOBDService getService() {
            return BluetoothOBDService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BluetoothOBDService", "Service started");
        // Display a notification about us starting.  We put an icon in the status bar.
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        // start location services
        driveKey = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Accelerometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // get supervisors' registration tokens (regTokens)
        regTokens = new ArrayList<String>();
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> supervisorIDs = new ArrayList<String>();

                // make list of supervisor IDs
                for (DataSnapshot child : dataSnapshot.child("drivers").child(uid).child("supervisorsIDs").getChildren()) {
                    supervisorIDs.add(child.getKey());
                }

                // get registration ID for each supervisor
                for(String id : supervisorIDs){
                    regTokens.add(dataSnapshot.child("regTokens").child(id).getValue(String.class));
                }
                // register listener once we have the values
                mSensorManager.registerListener(BluetoothOBDService.this, mAccelerometer, SensorManager.SENSOR_DELAY_UI, new Handler());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        // get parameters
        uid = intent.getStringExtra("userID");
        address = intent.getStringExtra("address");

        // make sure user has location permissions on
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // check for user permissions
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied.
                // start location updates
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                // empty callback we will use get last location in code
                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do nothing
                    };
                };
                // make sure we have permissions
                if (!(ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getApplicationContext(), "Location Services is disabled - drive canceled", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }

                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
                dbref = FirebaseDatabase.getInstance().getReference();

                // initiate connection
                // Cancel any thread attempting to make a connection
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }

                // Cancel any thread currently running a connection
                if (mConnectedThread != null) {
                    mConnectedThread.cancel();
                    mConnectedThread = null;
                }
                BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                // Start the thread to connect with the given device
                mConnectThread = new ConnectThread(dev, true);
                mConnectThread.start();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Location Services is disabled - drive canceled", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        });

        // do not restart service if fails
        return START_NOT_STICKY;
    }

/**
 * Return the current connection state.
 */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Return the current connection state.
     */
    public synchronized String getDriveKey() {
        return driveKey;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Handler mainHandler = new Handler(getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Failed to Connect to OBD", Toast.LENGTH_SHORT).show();
            }
        });
        this.stopSelf();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        synchronized (BluetoothOBDService.class) {
            if (!stopped) {
                Handler mainHandler = new Handler(getMainLooper());

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connection with OBD is lost", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            this.stopSelf();
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.OBDservice);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainDriverActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.app_logo)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.OBDservice))  // the label of the entry
                .setContentText("Tap to open app")  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        startForeground(NOTIFICATION,
                notification);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
    }

    public void onDestroy() {
        // stop all threads
        this.stop();
        // remove accelerometer lisetner
        mSensorManager.unregisterListener(this);
        // remove location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        // set drive as finished
        if (driveKey != null) {
            dbref.child("drives").child(driveKey).child("ongoing").setValue(false);
        }
        // reset all variables
        stopped = false;
        driveKey = null;

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            //android.os.Debug.waitForDebugger();  // this line is key
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);

            setName("ConnectThread" + mSocketType);

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                //android.os.Debug.waitForDebugger();
                mmSocket.connect();
                // Reset the ConnectThread because we're done
                synchronized (BluetoothOBDService.this) {
                    mConnectThread = null;
                }
                // Start the connected thread
                connected(mmSocket, mmDevice, mSocketType);
            }catch (Exception e1) {
                Log.d(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
                Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                try {
                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                    Object[] params = new Object[]{Integer.valueOf(1)};
                    BluetoothSocket sockFallback = (BluetoothSocket) m.invoke(mmSocket.getRemoteDevice(), params);
                    sockFallback.connect();
                    mmSocket = sockFallback;
                    // Reset the ConnectThread because we're done
                    synchronized (BluetoothOBDService.this) {
                        mConnectThread = null;
                    }
                    // Start the connected thread
                    connected(mmSocket, mmDevice, mSocketType);
                } catch (Exception e2) {
                    Log.d(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                    connectionFailed();
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private  Timer timer;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            //android.os.Debug.waitForDebugger();  // this line is key
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            try {
                // initiate obd general commands
                new EchoOffCommand().run(mmInStream, mmOutStream);
                new LineFeedOffCommand().run(mmInStream, mmOutStream);
                new TimeoutCommand(125).run(mmInStream, mmOutStream);
                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmInStream, mmOutStream);
                // location init
                if (!(ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(getApplicationContext(), "Location Services is disabled - drive canceled", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
                // create drive in db
                driveKey = dbref.child("drives").push().getKey();
                Drives newDrive = new Drives();
                newDrive.ongoing = true;
                newDrive.driverID = uid;
                newDrive.grade = 0;
                dbref.child("drives").child(driveKey).setValue(newDrive);
                // add initial measuremnt
                final DatabaseReference measRef = dbref.child("drives").child(driveKey).child("meas").child("0");
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        double lat = -1;
                        double longitude = -1;
                        if (location != null) {
                            lat = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                        // set start time
                        dbref.child("drives").child(driveKey).child("StartTimeStamp").setValue(-Calendar.getInstance().getTime().getTime());
                        measRef.setValue(new Measurement(0, lat, longitude, 0));
                    }});


            }
            catch (Exception e) {
                Log.e(TAG, "failed to initiate OBD general commands", e);
            }
        }

            public void run() {
                Log.i(TAG, "BEGIN mConnectedThread");
                // initiate speed and RPM commands and make mesauremnt each 5 seconds
                final RPMCommand rpmCMD = new RPMCommand();
                final SpeedCommand speedCMD = new SpeedCommand();
                // init location service

                this.timer = new Timer();
                this.timer.scheduleAtFixedRate(new TimerTask() {
                    int count = 1; //num of measurements
                    int NumOfPunish = 0; //num of punishments
                    float AverageSpeed = 0; //the average speed
                    @Override
                    public void run() {
                        try {
                            rpmCMD.run(mmInStream, mmOutStream);
                            speedCMD.run(mmInStream, mmOutStream);
                            // push to dB
                            if (!(ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                                Toast.makeText(getApplicationContext(), "Location Services is disabled - drive canceled", Toast.LENGTH_SHORT).show();
                                stopSelf();
                            }
                            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    double lat = -1;
                                    double longitude = -1;
                                    if (location != null) {
                                        lat = location.getLatitude();
                                        longitude = location.getLongitude();
                                    }
                                    int speed = speedCMD.getMetricSpeed();
                                    // add to db only if speed is above 0
                                    if (speed > 0) {
                                        int rpm = rpmCMD.getRPM();

                                        DatabaseReference measRef = dbref.child("drives").child(driveKey).child("meas").child(String.valueOf(count));
                                        count++;
                                        measRef.setValue(new Measurement(speed, lat, longitude, rpm));
                                        //this is the grading algorithm:
                                        NumOfPunish += SetPunishForBadResult(speed, rpm);
                                        AverageSpeed = (AverageSpeed*(count-1) + speed)/count;
                                        float Grade = OneGradingAlg(count, AverageSpeed, NumOfPunish, speed, rpm);
                                        dbref.child("drives").child(driveKey).child("grade").setValue(Grade);
                                    }
                                }
                            });

                        } catch (Exception ex) {
                            Log.e(TAG, "disconnected", ex);
                            this.cancel(); // cancel timer
                            stopped = false;
                            connectionLost();
                        }
                    }
                }, 2000, 2000);
        }

        public void cancel() {
            try {
                timer.cancel();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public static int SetPunishForBadResult(float CurrSpeed, float CurrRpm) {
        if (CurrSpeed>110 || CurrRpm>4000){ //high speed/rpm
            return 1;
        }
        if(CurrSpeed<30 && CurrRpm>3500){ //"drifting" - high acceleration
            return 1;
        }
        return 0;
    }

    public static float OneGradingAlg(int NumOfMeas, float AverageSpeed, int NumOfPunish, float CurrSpeed, float CurrRpm) {
        float Grade;
        if (AverageSpeed<=80) {
            Grade = AverageSpeed / 3;
        }
        else{
            Grade = AverageSpeed *2/3;
        }
        float PunishRate = NumOfPunish/NumOfMeas;
        Grade = Grade*(1+PunishRate);
        if (Grade>100){
            Grade=100;
        }
        return Grade;
    }
}