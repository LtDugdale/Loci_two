package com.lauriedugdale.loci.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lauriedugdale.loci.data.UserDatabase;
import com.lauriedugdale.loci.utils.DataUtils;
import com.lauriedugdale.loci.data.dataobjects.GeoEntry;
import com.lauriedugdale.loci.utils.GeofencingUtils;
import com.lauriedugdale.loci.utils.LocationUtils;

import java.util.ArrayList;

public class GeoFencingService extends Service implements OnCompleteListener<Void> {
    //TODO This class has two purposes - providing location to the "NearMeFragment" and Geofencing consider splitting
    //TODO Close this class shutting down all the open API connections
    //TODO Find a way to fetch location when app is not running
    //TODO mark in database an entry has been viewed
    public static final String TAG = "GeoFencingService";

    private static final long INTERVAL = 100000;
    private static final long FASTEST_INTERVAL = 50000;

    GoogleApiClient mGogleApiClient = null;

    // Provides access to the geofencing api
    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList; // List of geofences used
    private PendingIntent mGeofencePendingIntent; // Used when requesting to add or remove geofences

    private UserDatabase mUserDatabase;
    private Bundle mGeoEntries;

    private Location mLocation;
    private Location mOldLocation;

    public GeoFencingService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        mUserDatabase = new UserDatabase(this);
        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
        mGeoEntries = new Bundle();

        mGogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d(TAG, "Connected to GoogleApiClient");
                        startLocationMonitoring();
                        retrieveEntries();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Suspended connection to GoogleApiClient");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "Failed to connect to GoogleApiClient - " + connectionResult.getErrorMessage());
                    }
                }).build();

        mGogleApiClient.connect();


        mGeofencingClient = LocationServices.getGeofencingClient(this);

        return Service.START_NOT_STICKY;
    }



    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {

        } else {
            Log.w(TAG, "task unsuccessful");
        }
    }

    private void startLocationMonitoring(){
        Log.d(TAG, "startLocationMonitoring");
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(INTERVAL)
                    .setFastestInterval(FASTEST_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGogleApiClient,
                    locationRequest, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.d(TAG, "location update lat/long " + location.getLatitude() + "" + location.getLongitude());

                            setLocation(location);
//                            Intent intent = new Intent("location_update");
//                            intent.putExtra("latitude", location.getLatitude());
//                            intent.putExtra("longitude", location.getLongitude());
//                            LocalBroadcastManager.getInstance(GeoFencingService.this).sendBroadcast(intent);
                        }
                    });

        } catch (SecurityException e){
            Log.d(TAG, "SecurityException - " + e.getMessage());
        }
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    public void setLocation(Location location){

        if (mLocation == null){
            mLocation = location;
            retrieveEntries();
        } else if (LocationUtils.getDistanceInMeters(mLocation.getLatitude(), mLocation.getLongitude(), location.getLatitude(), location.getLongitude()) > 1609.34){
            mLocation.setLatitude(location.getLatitude());
            mLocation.setLongitude(location.getLongitude());
            retrieveEntries();
        }
    }


    public void retrieveEntries(){

        if (mLocation == null){
            return;
        }

        final String currentUID = mUserDatabase.getCurrentUID();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference("entry_location");
        GeoFire geoFire = new GeoFire(database);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()), 8); // 5 mile radius

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("entry_permission");
        final DatabaseReference eRef = FirebaseDatabase.getInstance().getReference("entries");

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {

                ref.child(currentUID + "/" + key).addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        String entryKey = dataSnapshot.getKey();
                        eRef.child(entryKey).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                GeoEntry entry = dataSnapshot.getValue(GeoEntry.class);
                                if (entry == null) {
                                    return;
                                }

                                if (!entry.getCreator().equals(currentUID)){
                                    addToGeofenceList(entry);
                                    // add the geofences
                                    addGeofences();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                Log.d(TAG, String.format("Key %s is no longer in the search area", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d(TAG, String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.d(TAG, "There was an error with this query: " + error);
            }
        });
    }

    private void addToGeofenceList(GeoEntry entry){

        mGeoEntries.putParcelable(entry.getEntryID(), entry);

        mGeofenceList.add(new Geofence.Builder()
                // identifies the geofence
                .setRequestId(entry.getEntryID())
                .setCircularRegion(
                        entry.getLatitude(),
                        entry.getLongitude(),
                        GeofencingUtils.GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GeofencingUtils.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                // track entry and exit of geofence
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceIntentService.class);
        intent.putExtra("entries", mGeoEntries);


        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (mGeofenceList.isEmpty()){
            return;
        }

        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void removeGeofences() {
        if (mGeofenceList.isEmpty()){
            return;
        }
        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//        googleApiClient.disconnect();
    }
}
