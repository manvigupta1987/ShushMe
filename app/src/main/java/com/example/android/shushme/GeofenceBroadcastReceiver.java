package com.example.android.shushme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by manvi on 24/8/17.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive called");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()){
            Log.e(TAG, String.format("Error code : %d", geofencingEvent.getErrorCode()));
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
        {
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        }else {
            Log.e(TAG, String.format("Unknown transition : %d", geofenceTransition));
            return;
        }
        sendNotification(context, geofenceTransition);
    }

    private void sendNotification(Context context, int transitionType){
        Intent notificationIntent = new Intent(context, MainActivity.class);

        //Add main activity to task stack as parent
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(notificationIntent);

        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                   .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));

        } else if(transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));

        }

        builder.setContentText(context.getString(R.string.touch_to_relaunch))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //issue the notification
        notificationManager.notify(0, builder.build());
    }

    /**
          * Changes the ringer mode on the device to either silent or back to normal
          *
          * @param context The context to access AUDIO_SERVICE
          * @param mode    The desired mode to switch device to, can be AudioManager.RINGER_MODE_SILENT or
          *                AudioManager.RINGER_MODE_NORMAL
          */

    private void setRingerMode(Context context, int mode){
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT < 24 ||
                (android.os.Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted())) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);
        }
    }
}
