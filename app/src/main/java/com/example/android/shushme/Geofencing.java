package com.example.android.shushme;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by manvi on 24/8/17.
 */

public class Geofencing implements ResultCallback<Status> {
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private List<Geofence> mGeofenceList;
    private PendingIntent mPendingIntent;
    private Circle geoFenceLimits;
    private Marker geoFenceMarker;

    public static final String TAG = Geofencing.class.getSimpleName();

    private static int EXPIRATION_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    private static int GEOFENCE_RADIUS = 30; // 50 meters

    public Geofencing(Context mContext, GoogleApiClient googleApiClient) {
        this.mContext = mContext;
        this.mGoogleApiClient = googleApiClient;
        mGeofenceList = new ArrayList<>();
        mPendingIntent = null;
    }

    public void updateGeofenceList(PlaceBuffer placeBuffer) {

        if (placeBuffer == null || placeBuffer.getCount() == 0) {
            return;
        }
        for (Place place : placeBuffer) {
            double latitude = place.getLatLng().latitude;
            double longitude = place.getLatLng().longitude;
            String placeId = place.getId();

            //Create geofence object and add latitude, longitude and radius.
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeId)
                    .setExpirationDuration(EXPIRATION_DURATION)
                    .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            mGeofenceList.add(geofence);
        }
    }

    public void registerAllGeofence() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()
                || mGeofenceList == null || mGeofenceList.size() == 0) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(this);
    }


    public void unregisterAllGeofence(){
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()){
            return;
        }
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient,getGeofencePendingIntent()).setResultCallback(this);
    }

    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder geofencingRequest = new GeofencingRequest.Builder();
        //We need to set initial trigger which tells what happens if device is already inside the geofence which we
        // are trying to register. Setting it to INITIAL_TRIGGER_ENTER indicates that if device is already inside the geofence,
        // trigger entry transition immediately.
        ///Setting it to INITIAL_TRIGGER_DWELL means trigger entry event only if the device has been inside the geofence for some
        // duration of time.
        geofencingRequest.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        geofencingRequest.addGeofences(mGeofenceList);
        return geofencingRequest.build();
    }

    public PendingIntent getGeofencePendingIntent(){
        //Reuse the pending intent, if we already have it.
        if(mPendingIntent!=null){
            return mPendingIntent;
        }

        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mPendingIntent;
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e(TAG, String.format("Error adding/removing geofence : %s",
                status.getStatus().toString()));
    }
}
