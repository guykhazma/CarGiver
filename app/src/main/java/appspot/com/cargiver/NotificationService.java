package appspot.com.cargiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Stav on 18/12/2017.
 */

public class NotificationService extends FirebaseMessagingService {

    private static String sender;

    public static void setSender(String senderName){
        sender = senderName;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Intent intent = new Intent(this, StartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT);
        Notification notification = new NotificationCompat.Builder(this)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(remoteMessage.getData().get("title"))
                .setContentText(remoteMessage.getData().get("body"))
                .setSmallIcon(R.drawable.app_logo)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .build();
        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.notify(123, notification);
    }

    // Send notification to multiple receivers
    public static void sendNotification(String msg, List<String> regTokens){
        CustomNotification notif = new CustomNotification();
        notif.setBody(msg);
        notif.setTitle(sender);
        notif.setTokens(regTokens);
        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("notification").setValue(notif);
    }

    // Send notification to a single receiver
    public static void sendNotification(String msg, String regToken) {
        List<String> regTokens = new ArrayList<String>();
        regTokens.add(regToken);
        sendNotification(msg, regTokens);
    }

}
