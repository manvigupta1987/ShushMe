package com.example.android.shushme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by manvi on 24/8/17.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive called");
    }
}
