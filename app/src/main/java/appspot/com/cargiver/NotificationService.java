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

    private static final String SERVER_KEY =
            "AAAAVPQRc4g:APA91bGx63EZBBC6CSjyvNUcu6zQ5tmx63OsLu3VWW3YZdZH-v6pOTN0yMG4QixljIVEoiDKwJum3mSp0bD--gsglpYX5wRa79IOC8SsJuU9IPCrmeTSRJB0RatkNmiGiNwzmewO5O8K";

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
    public static void sendNotification_old(String msg, List<String> regTokens){
        Gson gson = new Gson();
        Data data = new Data();
        data.setTitle(sender);
        data.setBody(msg);
        PostRequestData postRequestData = new PostRequestData();
        postRequestData.setRegistration_ids(regTokens);
        postRequestData.setData(data);
        String json = gson.toJson(postRequestData);
        String url = "https://fcm.googleapis.com/fcm/send";

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "key=" + SERVER_KEY)
                .post(body)
                .build();

        Callback responseCallBack = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("Fail Message", "fail");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.v("response", response.toString());
            }


        };
        okhttp3.Call call = client.newCall(request);
        call.enqueue(responseCallBack);
    }

    // Send notification to multiple receivers
    public static void sendNotification(String msg, List<String> regTokens){
        String[] tokens = regTokens.toArray(new String[regTokens.size()]);
        CustomNotification notif = new CustomNotification();
        notif.setBody(msg);
        notif.setTitle(sender);
        notif.setTokens(tokens);
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
