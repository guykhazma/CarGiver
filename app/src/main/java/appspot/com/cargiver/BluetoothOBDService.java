package appspot.com.cargiver;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;



public class BluetoothOBDService extends Service {
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
    // Binder given to clients
    private final IBinder mBinder = new BluetoothOBDBinder();

    private String address;
    private String uid;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // initiate connection
        // Cancel any thread attempting to make a connection
        //android.os.Debug.waitForDebugger();  // this line is key
        uid = intent.getStringExtra("userID");
        address = intent.getStringExtra("address");
        //android.os.Debug.waitForDebugger();  // this line is key
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
        // do not restart if fails
        return START_NOT_STICKY;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        this.stopSelf();
        // Send a failure message back to the Activity
        Handler mainHandler = new Handler(getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Failed to Connect to OBD", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        this.stopSelf();
        // Send a failure message back to the Activity
        Handler mainHandler = new Handler(getMainLooper());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Connection with OBD is lost", Toast.LENGTH_SHORT).show();
            }
        });
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
                .setSmallIcon(R.drawable.widget)  // the status icon
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

        // Send the name of the connected device back to the UI Activity
        /*Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();*/
    }

    public void onDestroy() {
        // stop all threads
        this.stop();
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
                } catch (Exception e2) {
                    Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                    // TODO: check if  mmSocket.close(); is needed
                    connectionFailed();
                    return;
                }
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothOBDService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
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
            }
            catch (Exception e) {
                Log.e(TAG, "failed to initiate OBD general commands", e);
            }
        }
            public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            // initiate speed and RPM commands
            RPMCommand rpmCMD= new RPMCommand();
            SpeedCommand speedCMD = new SpeedCommand();
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {

                try {
                    rpmCMD.run(mmInStream,mmOutStream);
                    speedCMD.run(mmInStream,mmOutStream);
                    // push to dB
                    rpmCMD.getFormattedResult();
                    speedCMD.getFormattedResult();
                    Thread.sleep(2000);
                    //num_of_meas ++;
                    //average_speed = (average_speed*(num_of_meas-1)+current_speed)/average_speed
                    //Grade = GradeThisMeas(current_speed, current_rpm);
                    //if (Grade>85){
                    // num_of_punish ++;
                    // }

                    //FinalGradeThisDrive(int NumOfMeas, float AverageSpeed, int NumOfPunish)
                    //GradeThisMeas(float speed, float rpm)
                    // if(currMeasurment.GForce>4){
                    //SendPushNotification() //TODO needs to be coded
                    //}

                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public float GradeThisMeas(float speed, float rpm){
        float Grade = 0;
        if (rpm > 4000){
            //we start from 80 which is already bad. if rpm is over 5k, we grade 100.
            Grade = rpm/50;
        }else if (speed>110){
            //we start from 83 which is already bad. if speed is over 130, we grade 100.
            Grade = speed*3/4;
        }else{
            //the speed and rpm are dependent so we take only speed
            //the motivation is that speed up to 80 will get great score,
            //speed from 80-95 will get good score
            //95+ will get bad score
            if (speed<=80) {
                Grade = speed / 3;
            }
            if(speed>80 && speed <=95){
                Grade = speed *2/3;
            }
            if(speed>95 && speed<110){
                Grade = speed *3/4;
            }
        }
        if (Grade>100){
            return 100;
        }
        return Grade;
    }

    public float FinalGradeThisDrive(int NumOfMeas, float AverageSpeed, int NumOfPunish) {
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

    public float OneGradingAlg(int NumOfMeas, float AverageSpeed, int NumOfPunish, float CurrSpeed, float CurrRpm) {
        float Grade;
        if (CurrSpeed>110 || CurrRpm>4000){
            NumOfPunish++;
        }
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