package com.sommayah.myprayertimes;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.sommayah.myprayertimes.data.PrayTime;

import org.joda.time.Chronology;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.IslamicChronology;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by sommayahsoliman on 2/25/16.
 */
public class Utility {

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public static float DEFAULT_LATLONG = 0F;
    public static final float LATMECCA = 21.4167F;
    public static final float LONGMECCA = 39.8167F;

    public static PrayTime mPrayTime;



    public static boolean isLocationLatLonAvailable(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(context.getString(R.string.pref_location_latitude))
                && prefs.contains(context.getString(R.string.pref_location_longitude));
    }

    public static boolean isAlarmEnabled(Context context){
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_alarm_enabled),
                true);

    }

    public static boolean isAlarmInitiated(Context context){
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_alarm_initiated),
                false);
    }

    public static float getLocationLatitude(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(context.getString(R.string.pref_location_latitude),
                DEFAULT_LATLONG);
    }

    public static float getLocationLongitude(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(context.getString(R.string.pref_location_longitude),
                DEFAULT_LATLONG);
    }

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static int getPreferredTimeFormat(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(context.getString(R.string.pref_time_format_key),
                PrayTime.TIME12);
    }

    public static String getLocationAddress(Context context, double longitude, double latitude){
        String TAG = "get Location Address";
        String address = "";
        String countryName = "";
        String cityName = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    // In this sample, get just a single address.
                    5);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e(TAG, context.getResources().getString(R.string.service_not_available), ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, context.getResources().getString(R.string.invalid_lat_long_used) + ". " +
                    "Latitude = " + latitude +
                    ", Longitude = " +
                    longitude, illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.d(TAG, context.getResources().getString(R.string.no_addresses_found));
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

        }
        return address;
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "Monday", "Tuesday", "wednesday".
     *
     * @return
     */
    public static String getDayName() {
        Time t = new Time();
        t.setToNow();
        Time time = new Time();
        time.setToNow();
        // Otherwise, the format is just the day of the week (e.g "Wednesday".
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
        return dayFormat.format(System.currentTimeMillis());

    }




    /**
     * Helper method to display today's date in a friendly display to user

     * @return a user-friendly representation of the date.

     */
    public static String getGregoreanDayString() {
        // The day string shows like that
        // "Mon Jun 8"
        //ss: check gmt on and off later
        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd");
        return dateFormat.format(currentTime);

    }

    public static String getHijriDate(Context context){
        Chronology iso = ISOChronology.getInstanceUTC();
        Chronology hijri = IslamicChronology.getInstanceUTC();

        LocalDate todayIso = new LocalDate();
        LocalDate todayHijri = new LocalDate(todayIso.toDateTimeAtStartOfDay(),
                hijri);
        int year = todayHijri.getYear();
        int day =  todayHijri.getDayOfMonth();
        int month = todayHijri.getMonthOfYear();
        String hijriDate = String.valueOf(day) + " "+ getHijriMonthName(month,context)+ " " + String.valueOf(year);
        return hijriDate;

    }

    public static void testPrayertimes(Context context){
        double latitude = 37.3533088;
        double longitude = -121.9871216;
        latitude = Double.valueOf(getLocationLatitude(context));
        longitude = Double.valueOf(getLocationLongitude(context));

        // Test Prayer times here
        PrayTime prayers = new PrayTime();
        double timezone = prayers.getBaseTimeZone();
        prayers.setTimeFormat(prayers.TIME12);
        prayers.setCalcMethod(prayers.ISNA);
        prayers.setAsrJuristic(prayers.SHAFII);
        prayers.setAdjustHighLats(prayers.ANGLEBASED);
        int[] offsets = {0, 0, 0, 0, 0, 0, 0}; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
        prayers.tune(offsets);

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        ArrayList<String> prayerTimes = prayers.getPrayerTimes(cal,
                latitude, longitude, timezone);
        ArrayList<String> prayerNames = prayers.getTimeNames();

        for (int i = 0; i < prayerTimes.size(); i++) {
            Log.d("Test prayer",(prayerNames.get(i) + " - " + prayerTimes.get(i)));
        }
    }

    public static ArrayList<String> getPrayTimes(Calendar cal,Context context){
        mPrayTime = new PrayTime();
        ArrayList<String> prayerTimes;
        mPrayTime.setTimeFormat(mPrayTime.TIME24); //determine 12 or 24 in prayeradapter
        mPrayTime.setCalcMethod(mPrayTime.ISNA);
        mPrayTime.setAsrJuristic(mPrayTime.SHAFII);
        mPrayTime.setAdjustHighLats(mPrayTime.ANGLEBASED);
        int[] offsets = {0, 0, 0, 0, 0, 0, 0}; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
        mPrayTime.tune(offsets);
        prayerTimes = mPrayTime.getPrayerTimes(cal,getLocationLatitude(context),
                getLocationLongitude(context),mPrayTime.getBaseTimeZone());
        prayerTimes.remove(5); // i don't need the maghrib and sunset just the sunset as maghrib
        return prayerTimes;
    }




    public static float getQiblaDirection(Context context){
        float longitude = getLocationLongitude(context);
        float latitude = getLocationLatitude(context);
        double longitudeR = Math.toRadians(longitude);
        double latitudeR = Math.toRadians(latitude);
        double longRMecca = Math.toRadians(LONGMECCA);
        double latRMecca = Math.toRadians(LATMECCA);
        double lonDeltaR = Math.toRadians(LONGMECCA - longitude);
        /* got formulat from: http://stackoverflow.com/questions/10887447/qibla-compass-in-android
         * changed them to radians */
        double y = Math.sin(lonDeltaR) * Math.cos(latRMecca);
        double x = Math.cos(latitudeR) * Math.sin(latRMecca)
                - Math.sin(latitudeR)  * Math.cos(latRMecca) * Math.cos(lonDeltaR);
        float bearing = (float)Math.toDegrees(Math.atan2(y,x));


        return bearing;
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /* This function get the name of the lunar month based on its order*/
    private static String getHijriMonthName(int month, Context context){
        String monthName = "unknown";
        switch (month){
            case 1:
                monthName = context.getString(R.string.muharam);
                break;
            case 2:
                monthName = context.getString(R.string.safar);
                break;
            case 3:
                monthName = context.getString(R.string.rabee31);
                break;
            case 4:
                monthName = context.getString(R.string.rabee32);
                break;
            case 5:
                monthName = context.getString(R.string.jumada1);
                break;
            case 6:
                monthName = context.getString(R.string.jumada2);
                break;
            case 7:
                monthName = context.getString(R.string.rajab);
                break;
            case 8:
                monthName = context.getString(R.string.shaban);
                break;
            case 9:
                monthName = context.getString(R.string.ramadan);
                break;
            case 10:
                monthName = context.getString(R.string.shawal);
                break;
            case 11:
                monthName = context.getString(R.string.dhulqe3da);
                break;
            case 12:
                monthName = context.getString(R.string.dhulqe3da);
                break;
            default:
                break;
        }
        return monthName;
    }

    public static String getPrayerName(int pos) {
        String name = "";
        switch (pos){
            case 0:
                name = "Fajr";
                break;
            case 1:
                name = "SunRise";
                break;
            case 2:
                name = "Zuhr";
                break;
            case 3:
                name = "Asr";
                break;
            case 4:
                name = "Maghrib";
                break;
            case 5:
                name = "Isha";
                break;
            default:
                name = "error";
                break;

        }
        return name;
    }
}
