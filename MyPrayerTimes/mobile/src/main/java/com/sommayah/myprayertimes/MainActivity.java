package com.sommayah.myprayertimes;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 4320000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    //    public static final int MIN_TIME = 1000 * 60 * 120; //two hours
//    public static final int MIN_DIST = 20000; //set to 20 kilometers
    LocationManager mLocationManager;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    @Bind(R.id.title)
    TextView mTitleText;
    @Bind(R.id.dateTextView)
    TextView mhijriDateText;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.imageView)
    ImageView backgroundImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        //check if rtl flip image
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                //in Right To Left layout
                backgroundImage.setScaleX(-1);
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), CompassActivity.class);
                startActivity(intent);
            }
        });

        buildGoogleApiClient();

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
        mhijriDateText.setText(Utility.getHijriDate(getApplicationContext()));
        boolean isManualLocation = Utility.isManualLocation(getApplicationContext());
        if (isManualLocation == false) {
            if (Utility.isLocationLatLonAvailable(getApplicationContext())) {
                mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
            }
        } else { //set manual location
            mTitleText.setText(Utility.getManualLocation(getApplicationContext()));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d(TAG, "no permission");
            Utility.setLocationStatus(getApplicationContext(), Utility.LOCATION_STATUS_PERMISSION_DENIED);
            mTitleText.setText(getString(R.string.location_not_found));

        } else {
            if (isManualLocation == false) {
                //check if location setting is on if we don't have good location, if not open settings to enable
                //checkLocationSettings();
            }
            //  mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, mLocationListener);
        }
        int temp = getResources().getIdentifier("backgroundmosque" + Utility.getNextPos(this), "drawable", getPackageName());
        backgroundImage.setImageResource(temp);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void makeUseOfNewLocation(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String address = Utility.getLocationAddress(getApplicationContext(), longitude, latitude);
        float ulong = Utility.getLocationLongitude(getApplicationContext());
        float ulat = Utility.getLocationLatitude(getApplicationContext());
        if (!((Math.abs((float) longitude - Utility.getLocationLongitude(getApplicationContext())) < 0.01)
                && (Math.abs((float) latitude - Utility.getLocationLatitude(getApplicationContext())) < 0.01))) { //check if it is really new location
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pref_location_key), address);
            editor.putFloat(getString(R.string.pref_location_latitude),
                    (float) latitude);
            editor.putFloat(getString(R.string.pref_location_longitude),
                    (float) longitude);
            editor.commit();
            if (Utility.isLocationLatLonAvailable(getApplicationContext())) {
                mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
            }
            if (Utility.isAlarmEnabled(getApplicationContext())) {
                PrayerAlarmReceiver alarm = new PrayerAlarmReceiver();
                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                new LoadPrayersAsyncTask(this, cal).execute();
                alarm.cancelAlarm(getApplicationContext());
                alarm.addPrayerAlarm(getApplicationContext());
            }
            int temp = getResources().getIdentifier("backgroundmosque" + Utility.getNextPos(this), "drawable", getPackageName());
            backgroundImage.setImageResource(temp);
        }

    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "no permission granted yet");
            Utility.setLocationStatus(getApplicationContext(), Utility.LOCATION_STATUS_UNKNOWN);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))) {
                // If the user has previously denied permission , and do not check marked " never show this warning
                // We can show an alert explaining to the user because permission is important.
                Log.d(TAG, "permissions are denied");
                Utility.setLocationStatus(getApplicationContext(), Utility.LOCATION_STATUS_PERMISSION_DENIED);
                mTitleText.setText(getString(R.string.location_not_found));
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            if (!Utility.isManualLocation(getApplicationContext())) {
                makeUseOfNewLocation(mLastLocation);
                Log.d("known location long", String.valueOf(mLastLocation.getLongitude()));
                Log.d("known location lat", String.valueOf(mLastLocation.getLatitude()));
            }
        } else {
            //location null no previous location
            Log.d(TAG, "last known location null");
            Utility.setLocationStatus(getApplicationContext(), Utility.LOCATION_STATUS_UNKNOWN);
        }
        startLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Utility.setLocationStatus(getApplicationContext(), Utility.LOCATION_STATUS_UNKNOWN);
    }

    @Override
    public void onLocationChanged(Location location) {
        makeUseOfNewLocation(location);
    }


}
