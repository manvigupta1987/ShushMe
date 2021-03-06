package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 101;
    private static final int REQUEST_PLACE_PICKER_CODE = 102;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private  GoogleApiClient mGoogleApiClient;
    private boolean mIsEnabled = false;
    private Geofencing mGeofencing;
    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this,null);
        mRecyclerView.setAdapter(mAdapter);


        Switch onOffSwitch = (Switch)findViewById(R.id.enable_switch);
        mIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled),false);
        onOffSwitch.setChecked(mIsEnabled);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled),isChecked);
                mIsEnabled = isChecked;
                editor.commit();
                if(mIsEnabled){
                    mGeofencing.registerAllGeofence();
                }else {
                    mGeofencing.unregisterAllGeofence();
                }

            }
        });

        // TODO (4) Create a GoogleApiClient with the LocationServices API and GEO_DATA_API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                            .addApi(LocationServices.API)
                                            .addApi(Places.GEO_DATA_API)
                                            .addConnectionCallbacks(this)
                                            .addOnConnectionFailedListener(this)
                                            .enableAutoManage(this, this)
                                            .build();
        mGeofencing = new Geofencing(this, mGoogleApiClient);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"Google Api client is connected");
        refreshPlacesData();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"Google Api client is suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG,"Google Api client connection is failed");
    }

    //This function is called to reterive all the locally stored placeIDs and get those place id details from google using
    //getPlaceById method.
    public void refreshPlacesData(){
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor data = getContentResolver().query(uri, null, null, null, null, null);

        if(data == null || data.getCount() ==0 ){
            return;
        }

        List<String> placeIds = new ArrayList<>();
        while (data.moveToNext()){
            placeIds.add(data.getString(data.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        //getPlaceById is used to retrieves the information about locally stored place ids from google live server.
        PendingResult<PlaceBuffer> pendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient,placeIds.toArray(new String[placeIds.size()]));
        pendingResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.swapPlaces(places);
                mGeofencing.updateGeofenceList(places);
                if(mIsEnabled){
                    mGeofencing.registerAllGeofence();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        CheckBox locationPermissions = (CheckBox)findViewById(R.id.location_permission_checkbox);
        CheckBox ringerPermissions = (CheckBox)findViewById(R.id.ringer_permissions_checkbox);
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
            locationPermissions.setChecked(false);
        }else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Check if the API supports such permission change and check if permission is granted
        if (android.os.Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
            ringerPermissions.setChecked(false);
        } else {
            ringerPermissions.setChecked(true);
            ringerPermissions.setEnabled(false);
        }

    }

    public void onLocationPermissionClicked(View view){
        ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                            PERMISSIONS_REQUEST_FINE_LOCATION);
    }
    // TODO (9) Implement the Add Place Button click event to show  a toast message with the permission status

    public void onAddPlaceButtonClicked(View view){
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,getString(R.string.need_location_permission_message), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this,getString(R.string.location_permissions_granted_message), Toast.LENGTH_SHORT).show();
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            Intent intent = builder.build(this);
            startActivityForResult(intent,REQUEST_PLACE_PICKER_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, "google play service not working " + e.getMessage());
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "google play service not available " + e.getMessage());
        } catch (Exception e){
            Log.e(TAG, "exception happens" + e.getMessage());
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_PLACE_PICKER_CODE && resultCode == RESULT_OK){
            Place place = PlacePicker.getPlace(this,data);

            if(place == null){
                Log.i(TAG,"No Place is selected");
                return;
            }

            String placeAddress = place.getAddress().toString();
            String placeID = place.getId();
            String placeName = place.getName().toString();

            ContentValues contentValues = new ContentValues();
            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);

            refreshPlacesData();
        }
    }

    public void onRingerPermissionsClicked(View view){
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }
}
