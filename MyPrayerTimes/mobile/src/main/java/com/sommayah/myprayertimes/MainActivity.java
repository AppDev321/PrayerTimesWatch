package com.sommayah.myprayertimes;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int MIN_TIME =0/*1000 * 60 * 20*/; //twenty minutes
    public static final int MIN_DIST = 3000; //set to 3 kilometers
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    TextView mTitleText;
    TextView mhijriDateText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(getApplicationContext(),CompassActivity.class);
                //ss:add the angle of prayer later with the intent
                startActivity(intent);


            }
        });

        String day = Utility.getDayName();
        String date = Utility.getGregoreanDayString();
        String hDate = Utility.getHijriDate(getApplicationContext());
        Log.d("day of week:", day);
        Log.d("date:", date);
        Log.d("hijri date:", hDate);


        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        //String locationProvider = LocationManager.NETWORK_PROVIDER;
        String locationProvider = LocationManager.GPS_PROVIDER;


        //ss: check here if location is null
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d(TAG, "no network available");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION) &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))) {
                // If the user has previously denied permission , and do not check marked " never show this warning
                // We can show an alert explaining to the user because permission is important.
                Log.d(TAG,"permissions are denied");
            } else {

                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0);
            }
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        else {
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);
            if(lastKnownLocation != null){
                Log.d("known location long", String.valueOf(lastKnownLocation.getLongitude()));
                Log.d("known location lat", String.valueOf(lastKnownLocation.getLatitude()));
            }else{
                //location null no previous location
                Log.d(TAG,"last known location null");
            }
        }

        Utility.testPrayertimes(getApplicationContext());



    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Utility.isLocationLatLonAvailable(getApplicationContext())) {
            mTitleText = (TextView) findViewById(R.id.title);
            mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
            mhijriDateText = (TextView) findViewById(R.id.dateTextView);
            mhijriDateText.setText(Utility.getHijriDate(getApplicationContext()));
        }
        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.d(TAG, "no network available2");

        }else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, mLocationListener);
        }
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
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        String address = "";
        String countryName = "";
        String cityName = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    // In this sample, get just a single address.
                    5);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e(TAG, getString(R.string.service_not_available), ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, getString(R.string.invalid_lat_long_used) + ". " +
                    "Latitude = " + latitude +
                    ", Longitude = " +
                    longitude, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.d(TAG, getString(R.string.no_addresses_found));
            // If the provided place doesn't have an address, we'll form a display-friendly
            // string from the latlng values.
            address = String.format("(%.2f, %.2f)",latitude, longitude);
        } else {
            for (Address addr : addresses) {
//                String result = addr.getAdminArea() != null ? addr.getAdminArea() : "?";
//                result += " | ";
//                result += addr.getSubAdminArea() != null ? addr.getSubAdminArea() : "?";
//                result += " | ";
//                result += addr.getLocality() != null ? addr.getLocality() : "?";
//                result += " | ";
//                result += addr.getSubLocality() != null ? addr.getSubLocality() : "?";
//                result += " | ";
//                result += addr.getThoroughfare() != null ? addr.getThoroughfare() : "?";
//                result += " | ";
//                result += addr.getSubThoroughfare() != null ? addr.getSubThoroughfare() : "?";
//                Log.i(TAG, result);
                countryName = addr.getCountryName();
                if(cityName == null || cityName.equals("")) {
                    cityName = addr.getLocality();
                }if(cityName == null || cityName.equals("")){
                    cityName = addr.getSubAdminArea();
                }if(cityName == null || cityName.equals("")){
                    cityName = addr.getAdminArea();
                }
                Log.i(TAG, "country: "+ countryName +" ,city: "+ cityName);
                address = cityName +", " + countryName;
                if(cityName != null  && countryName != null) //we got one good address
                    break;
            }
            
            Log.i(TAG, getString(R.string.address_found));
            //cityName = addresses.get(0).getAddressLine(0);
            //String stateName = addresses.get(0).getAddressLine(1);
            //countryName = addresses.get(0).getAddressLine(2);
            Log.d(TAG, addresses.get(0).toString());
        }



        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.pref_location_key), address);

        // Also store the latitude and longitude so that we can use these to get a precise
        // result from our weather service. We cannot expect the weather service to
        // understand addresses that Google formats.
        editor.putFloat(getString(R.string.pref_location_latitude),
                (float) latitude);
        editor.putFloat(getString(R.string.pref_location_longitude),
                (float) longitude);
        editor.commit();
        Log.d("location long:", String.valueOf(location.getLongitude()));
        Log.d("location lat", String.valueOf(location.getLatitude()));
        Utility.testPrayertimes(getApplicationContext());
        if(Utility.isLocationLatLonAvailable(getApplicationContext())) {
            mTitleText = (TextView) findViewById(R.id.title);
            mTitleText.setText(Utility.getPreferredLocation(getApplicationContext()));
        }
    }
}