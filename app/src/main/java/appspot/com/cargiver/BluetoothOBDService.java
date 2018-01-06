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
import android.location.LocationManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BluetoothOBDService extends Service implements SensorEventListener {
    // Accelerometer variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float mAccelCurrent; // current acceleration

    private static final String TAG = "BluetoothOBDService";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private NotificationManager mNM;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    public volatile int mState;
    private int NOTIFICATION = R.string.OBDservice;

    // location
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    // Binder given to clients
    private final IBinder mBinder = new BluetoothOBDBinder();


    private List<String> regTokens;
    private String userName;
    private BluetoothDevice dev;
    private String address;
    private String uid;
    private String driveKey;
    public volatile boolean stopped; // indicates whether failure caused the problem
    // indicates if restart was done
    public static volatile int numRestart;
    public static volatile boolean restart; // indicate whether the restart is after a succesfull connection

    // grade
    int count = 1; //num of measurements
    int NumOfPunish = 0; //num of punishments
    float AverageSpeed = 0; //the average speed
    Location lastLocation;

    // broadcast tags
    public static String connectionFailedBroadcastIntent = "com.OBDService.ConnectionFailed";
    public static String connectionLostBroadcastIntent = "com.OBDService.ConnectionLost";
    public static String connectionConnectedBroadcastIntent = "com.OBDService.Connected";
    public static String driveFinishedBroadcastIntent = "com.OBDService.driveFinished";
    public static String errorOccurredBroadcastIntent = "com.OBDService.errorOcurred";
    public static String permissionsErrorBroadcastIntent = "com.OBDService.permissionsError";
    private  static volatile ScheduledExecutorService scheduler;


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
        float x = event.values[0]/(float)9.81;
        float y = event.values[1]/(float)9.81;
        float z = event.values[2]/(float)9.81;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));

        // Send notification in case of crash
        if(mAccelCurrent >= 3.5){
            String msg = "Possible crash detected! "+userName;
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

        // get parameters
        uid = intent.getStringExtra("userID");
        address = intent.getStringExtra("address");

        // get supervisors' registration tokens (regTokens)
        regTokens = new ArrayList<String>();
        dbref = FirebaseDatabase.getInstance().getReference();
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get user's name
                userName = dataSnapshot.child("users").child(uid).getValue(User.class).username;

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


        // make sure user has location permissions on
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // check for user permissions
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied.
                // make sure we have permissions
                if (!(ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    // failure message for failing to get location
                    Intent intent = new Intent(BluetoothOBDService.permissionsErrorBroadcastIntent);
                    LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcastSync(intent);
                    stopSelf();
                }
                // start location updates
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                // empty callback we will use get last location in code
                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do nothing
                    };
                };


                mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);

                // reset variables
                count = 1; //num of measurements
                NumOfPunish = 0; //num of punishments
                AverageSpeed = 0; //the average speed

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
                dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                // Start the thread to connect with the given device
                numRestart = 0;
                mConnectThread = new ConnectThread(dev, true);
                mConnectThread.start();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // failure message for failing to get location
                Intent intent = new Intent(BluetoothOBDService.permissionsErrorBroadcastIntent);
                LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcastSync(intent);
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
        if (!restart) {
            // failure message for failing to connect on
            Intent intent = new Intent(BluetoothOBDService.connectionFailedBroadcastIntent);
            LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);
        }
        this.stopSelf();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        if (!stopped) {
            this.mState = STATE_NONE;
            Intent intent = new Intent(BluetoothOBDService.connectionLostBroadcastIntent);
            LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);
        }
        this.stopSelf();
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
        this.mState = STATE_NONE;
        this.stop();
        // remove accelerometer listener
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        // remove location updates
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
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
            // try connecting multiple times
            while (numRestart < 4) {
                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();
                    // Reset the ConnectThread because we're done
                    synchronized (BluetoothOBDService.this) {
                        mConnectThread = null;
                    }
                    // Start the connected thread
                    connected(mmSocket, mmDevice, mSocketType);
                } catch (Exception e1) {
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
                        return; // finish current thread
                    } catch (Exception e2) {
                        Log.d(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                        if (numRestart > 2) {
                            // send message accroding to failure type
                            if (!restart) {
                                connectionFailed();
                            }
                            else {
                                connectionLost();
                            }
                            return;
                        }
                        else {
                            numRestart++;
                            Log.d(TAG, "Restart" + numRestart);
                            try{
                                Thread.sleep(4000);
                            } catch(InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
                // try sending OBD command to make sure this is an OBD device

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
                    // failure message for failing to get location
                    Intent intent = new Intent(BluetoothOBDService.permissionsErrorBroadcastIntent);
                    LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcastSync(intent);
                    stopSelf();
                }
                // if it is not a restart create drive in db
                if (!restart) {
                    driveKey = dbref.child("drives").push().getKey();
                    Drives newDrive = new Drives();
                    newDrive.ongoing = true;
                    newDrive.driverID = uid;
                    newDrive.grade = 0;
                    dbref.child("drives").child(driveKey).setValue(newDrive);
                    // set as connected
                    Intent intent = new Intent(BluetoothOBDService.connectionConnectedBroadcastIntent);
                    LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcast(intent);
                    // add initial measuremnt
                    final DatabaseReference measRef = dbref.child("drives").child(driveKey).child("meas").child("0");

                    mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double lat = location.getLatitude();
                                double longitude = location.getLongitude();
                                // set start time
                                dbref.child("drives").child(driveKey).child("StartTimeStamp").setValue(-Calendar.getInstance().getTime().getTime());
                                measRef.setValue(new Measurement(0, lat, longitude, 0));
                                lastLocation = location;
                            }
                            // if somehow error occurred
                            else {
                                // failure message for failing to get location
                                Intent intent = new Intent(BluetoothOBDService.errorOccurredBroadcastIntent);
                                LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcastSync(intent);
                                stopSelf();
                            }

                        }
                    });
                }
                // if it is a restarted drive indicate restart has succeeded
                else {
                    numRestart = 0;
                }
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
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate
                        (new Runnable() {
                            public void run() {
                                try {
                                    rpmCMD.run(mmInStream, mmOutStream);
                                    speedCMD.run(mmInStream, mmOutStream);
                                    // push to dB
                                    if (!(ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                                        // failure message for failing to get location
                                        Intent intent = new Intent(BluetoothOBDService.permissionsErrorBroadcastIntent);
                                        LocalBroadcastManager.getInstance(BluetoothOBDService.this).sendBroadcastSync(intent);
                                        stopSelf();
                                    }
                                    mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                            // insert only valid locations
                                            if (location != null && driveKey != null) {
                                                double lat = location.getLatitude();
                                                double longitude = location.getLongitude();

                                                int speed = speedCMD.getMetricSpeed();
                                                int rpm = rpmCMD.getRPM();
                                                DatabaseReference measRef = dbref.child("drives").child(driveKey).child("meas").child(String.valueOf(count));
                                                count++;
                                                // if current location is far then 20m then last location update
                                                // avoid a case where we have points in radius less then 200 meters
                                                if (isBetterLocation(location, lastLocation) && location.distanceTo(lastLocation) > 100) {
                                                    // put new in db and update
                                                    measRef.setValue(new Measurement(speed, lat, longitude, rpm));
                                                    lastLocation = location;
                                                }
                                                else {
                                                    Log.e(TAG, "in");
                                                    measRef.setValue(new Measurement(speed, lastLocation.getLatitude(), lastLocation.getLongitude(), rpm));
                                                }
                                                // update grade only if speed is above 0
                                                if (speed > 0) {
                                                    //this is the grading algorithm:
                                                    NumOfPunish += SetPunishForBadResult(speed, rpm);
                                                    AverageSpeed = (AverageSpeed * (count - 1) + speed) / count;
                                                    float Grade = OneGradingAlg(count, AverageSpeed, NumOfPunish, speed, rpm);
                                                    dbref.child("drives").child(driveKey).child("grade").setValue(Grade);
                                                }
                                            }
                                        }
                                    });

                                } catch (Exception ex) {
                                    // try restarting
                                    if (ex instanceof IOException) {
                                        // stop all active
                                        // Start the thread to connect with the given device
                                        Log.e(TAG, "attempting restart", ex);
                                        scheduler.shutdown();
                                        try {
                                            scheduler.awaitTermination(2, TimeUnit.SECONDS);
                                        } catch (InterruptedException exp) {

                                        }
                                        numRestart++;
                                        restart = true; // changes only once to true
                                        mConnectThread = new ConnectThread(dev, true);
                                        mConnectThread.start();
                                        return; // close current thread
                                    } else {
                                        scheduler.shutdown();
                                        Log.e(TAG, "disconnected", ex);
                                        try {
                                            scheduler.awaitTermination(2, TimeUnit.SECONDS);
                                        } catch (InterruptedException exp) {

                                        }
                                        stopped = false;
                                        connectionLost();
                                    }
                                }
                            }
                        }, 2, 3, TimeUnit.SECONDS);
        }

        public void cancel() {
            try {
                scheduler.shutdown();
                try {
                    scheduler.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException exp) {

                }
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

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        if (location.getAccuracy() > 10)
            return false;
        
        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}