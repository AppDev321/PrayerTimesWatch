package com.sommayah.myprayertimes;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
import android.support.v7.app.AlertDialog;

import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;

import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int MIN_TIME =1000 * 60 * 120; //two hours
    public static final int MIN_DIST = 20000; //set to 20 kilometers
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    @Bind(R.id.title) TextView mTitleText;
    @Bind(R.id.dateTextView) TextView mhijriDateText;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;
    @Bind(R.id.imageView)ImageView backgroundImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                Intent intent = new Intent(getApplicationContext(),CompassActivity.class);
                startActivity(intent);


            }
        });

        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //String locationProvider = LocationManager.NETWORK_PROVIDER;
        String locationProvider = LocationManager.GPS_PROVIDER;
        //ss: check here if location is null
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d(TAG, "no permission granted yet");
            Utility.setLocationStatus(getApplicationContext(),Utility.LOCATION_STATUS_UNKNOWN);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))) {
                // If the user has previously denied permission , and do not check marked " never show this warning
                // We can show an alert explaining to the user because permission is important.
                Log.d(TAG, "permissions are denied");
                Utility.setLocationStatus(getApplicationContext(),Utility.LOCATION_STATUS_PERMISSION_DENIED);
                mTitleText.setText(getString(R.string.location_not_found));
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
//               public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                                      int[] grantResults)
//             to handle the case where the user grants the permission. See the documentation
//             for ActivityCompat#requestPermissions for more details.

        } else {
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);

            if (lastKnownLocation != null) {
                if(!Utility.isManualLocation(getApplicationContext())) {
                    makeUseOfNewLocation(lastKnownLocation);
                    Log.d("known location long", String.valueOf(lastKnownLocation.getLongitude()));
                    Log.d("known location lat", String.valueOf(lastKnownLocation.getLatitude()));
                }
            } else {
                //location null no previous location
                Log.d(TAG, "last known location null");
                Utility.setLocationStatus(getApplicationContext(),Utility.LOCATION_STATUS_UNKNOWN);
            }
        }



        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if(!Utility.isManualLocation(getApplicationContext())){
                    makeUseOfNewLocation(location);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG,"on status changed");
            }

            public void onProviderEnabled(String provider) {
                Log.d(TAG,"on provider enabled");
            }

            public void onProviderDisabled(String provider) {
                Log.d(TAG,"on provider disabled");
            }

        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mhijriDateText.setText(Utility.getHijriDate(getApplicationContext()));
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isManualLocation = prefs.getBoolean(getApplicationContext().getString(R.string.pref_loc_manual_set), false);
        if(isManualLocation == false) {
            if (Utility.isLocationLatLonAvailable(getApplicationContext())) {
                mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
            }
        }else{ //set manual location
            mTitleText.setText(Utility.getManualLocation(getApplicationContext()));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d(TAG, "no permission");
            Utility.setLocationStatus(getApplicationContext(),Utility.LOCATION_STATUS_PERMISSION_DENIED);
            mTitleText.setText(getString(R.string.location_not_found));

        }else {
            if(isManualLocation == false){
                //check if location setting is on if we don't have good location, if not open settings to enable
                checkLocationSettings();
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, mLocationListener);
        }
        int temp = getResources().getIdentifier("backgroundmosque"+Utility.getNextPos(this), "drawable", getPackageName());
        backgroundImage.setImageResource(temp);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //location cannot be caught

        }else {
            mLocationManager.removeUpdates(mLocationListener);
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
        int temp = getResources().getIdentifier("backgroundmosque"+Utility.getNextPos(this), "drawable", getPackageName());
        backgroundImage.setImageResource(temp);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String address = Utility.getLocationAddress(getApplicationContext(),longitude,latitude);
        if(!((float)longitude == Utility.getLocationLongitude(getApplicationContext())
                && (float)latitude == Utility.getLocationLatitude(getApplicationContext()))){ //check if it is really new location
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.pref_location_key), address);
            editor.putFloat(getString(R.string.pref_location_latitude),
                    (float) latitude);
            editor.putFloat(getString(R.string.pref_location_longitude),
                    (float) longitude);
            editor.commit();
            if(Utility.isLocationLatLonAvailable(getApplicationContext())) {
                mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
            }
            if (Utility.isAlarmEnabled(getApplicationContext())) {
                PrayerAlarmReceiver alarm = new PrayerAlarmReceiver();
                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                new LoadPrayersAsyncTask(this,cal).execute();
                alarm.cancelAlarm(getApplicationContext());
                alarm.addPrayerAlarm(getApplicationContext());
            }
        }

    }

    public void checkLocationSettings(){
        if(Utility.getPreferredLocation(getApplicationContext()).equals("")) {
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
            }

            try {
                network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (Exception ex) {
            }

            if (!gps_enabled && !network_enabled) {
                // notify user
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
                dialog.setPositiveButton(getResources().getString(R.string.enable), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                        //get gps
                    }

                });
                dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                        Utility.setLocationStatus(getApplicationContext(),Utility.LOCATION_STATUS_DISABLED);
                        mTitleText.setText(getString(R.string.location_not_found));

                    }
                });
                dialog.show();
            }
        }
    }

}
