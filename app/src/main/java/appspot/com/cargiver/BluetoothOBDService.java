package appspot.com.cargiver;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Vector;

public class BluetoothOBDService extends Service {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BluetoothOBDService.class.getName();
    private static final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    // threads
    private static ConnectThread connect = null;

    // info visible to other activities/fragments
    public static BluetoothDevice dev = null;
    public static BluetoothSocket sock = null;
    public static boolean connected = false;
    public static boolean isConnecting = false;


    /**
     * Inititate bluetooth connection with a device
     * @param isSecure
     */
    public static void connect(BluetoothDevice device, boolean isSecure) {
        // avoid connecting multiple times when one is already running
        if (isConnecting) {
            return;
        }
        synchronized (BluetoothOBDService.class) {
            isConnecting = true;
            connect = new ConnectThread(device, true);
        }
        // call to connect thread
        connect.start();
    }

    public static String mac; // the mac used for communication

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private NotificationManager mNM;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        /*
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LocalServiceActivities.Controller.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);*/
    }

    // thread for handling connection
    private static class ConnectThread extends Thread {
        private  BluetoothSocket sock = null;
        private  BluetoothSocket sockFallback = null;
        private  BluetoothDevice device = null;
        private String info;
        private String address;
        private boolean secure;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            this.secure = secure;
            this.device = device;
        }

        @Override
        public void run() {
            Log.e(TAG, "In thread");
            try {
                if (secure) {
                    try {
                        sock = device.createRfcommSocketToServiceRecord(MY_UUID);
                        sock.connect();
                        // if succeed update connection info
                        synchronized (BluetoothOBDService.class) {
                            BluetoothOBDService.connected = true;
                            BluetoothOBDService.dev = device;
                            BluetoothOBDService.sock = sock;
                        }
                    } catch (Exception e1) {
                        Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
                        Class<?> clazz = sock.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                        try {
                            Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                            Object[] params = new Object[]{Integer.valueOf(1)};
                            sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                            sockFallback.connect();
                            sock = sockFallback;
                            // if succeed update connection info
                            synchronized (BluetoothOBDService.class) {
                                BluetoothOBDService.connected = true;
                                BluetoothOBDService.dev = device;
                                BluetoothOBDService.sock = sock;
                            }
                        } catch (Exception e2) {
                            Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                            throw new IOException(e2.getMessage());
                        }
                    }
                } else {
                    try {
                        sock = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        sock.connect();
                        // if succeed update connection info
                        synchronized (BluetoothOBDService.class) {
                            BluetoothOBDService.connected = true;
                            BluetoothOBDService.dev = device;
                            BluetoothOBDService.sock = sock;
                        }
                    } catch (Exception e1) {
                        Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
                        Class<?> clazz = sock.getRemoteDevice().getClass();
                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                        // TODO: add to service
                        try {
                            Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                            Object[] params = new Object[]{Integer.valueOf(1)};
                            sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                            sockFallback.connect();
                            sock = sockFallback;
                            // if succeed update connection info
                            synchronized (BluetoothOBDService.class) {
                                BluetoothOBDService.connected = true;
                                BluetoothOBDService.dev = device;
                                BluetoothOBDService.sock = sock;
                            }
                        } catch (Exception e2) {
                            Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                            throw new IOException(e2.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothOBDService.class) {
                connect = null;
                isConnecting = false;
            }
        }
    }
}