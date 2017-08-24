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
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.security.Permission;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 101;
    private static final int REQUEST_PLACE_PICKER_CODE = 102;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;

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
        mAdapter = new PlaceListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        // TODO (4) Create a GoogleApiClient with the LocationServices API and GEO_DATA_API
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                                            .addApi(LocationServices.API)
                                            .addApi(Places.GEO_DATA_API)
                                            .addConnectionCallbacks(this)
                                            .addOnConnectionFailedListener(this)
                                            .enableAutoManage(this, this)
                                            .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"Google Api client is connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"Google Api client is suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG,"Google Api client connection is failed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckBox checkBox = (CheckBox)findViewById(R.id.location_permission_checkbox);
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED){
            checkBox.setChecked(false);
        }else {
            checkBox.setChecked(true);
            checkBox.setEnabled(false);
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
        }
    }
}
