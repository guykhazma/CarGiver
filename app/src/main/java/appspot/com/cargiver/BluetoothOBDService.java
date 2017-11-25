package appspot.com.cargiver;

import android.app.IntentService;
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
import java.util.UUID;
import java.util.Vector;

public class BluetoothOBDService extends Service {

    public static String mac; // the mac used for communication

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}