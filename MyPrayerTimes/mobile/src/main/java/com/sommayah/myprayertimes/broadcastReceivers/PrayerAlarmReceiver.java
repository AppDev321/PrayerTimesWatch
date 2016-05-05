package com.sommayah.myprayertimes.broadcastReceivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.sommayah.myprayertimes.LoadPrayersAsyncTask;
import com.sommayah.myprayertimes.R;
import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.data.PrayerContract;
import com.sommayah.myprayertimes.dataModels.Prayer;
import com.sommayah.myprayertimes.services.PrayerNotificationService;
import com.sommayah.myprayertimes.services.UpdateWatchIntentService;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class PrayerAlarmReceiver extends WakefulBroadcastReceiver{
    public final String LOG_TAG = PrayerAlarmReceiver.class.getSimpleName();
    public static final String ACTION_PRAYER_TIME_ALARM = "com.sommayah.myprayertimes.ACTION_PRAYER_TIME_ALARM";
    public static final String EXTRA_PRAYER_NAME = "prayer_name";
    public static final String EXTRA_PRAYER_TIME = "prayer_time";
    public static final int ALARM_ID = 1000;
    public static final int PASSIVE_LOCATION_ID = 2000;
    public static final int FIVE_MIN = 5000 *60;
    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmManager;
    private Prayer next_prayer_time;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;
    public PrayerAlarmReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Start the service, keeping the device awake while it is launching.
        String prayerName =  intent.getStringExtra(EXTRA_PRAYER_NAME);
        long alarmTime = intent.getLongExtra(EXTRA_PRAYER_TIME, -1);
        if(prayerName == null)
            prayerName = ""; // in case error in prayer name
        if (alarmTime != -1 && Math.abs(alarmTime - System.currentTimeMillis()) < FIVE_MIN) {
            Intent sendNotificationIntent = new Intent(context, PrayerNotificationService.class);
            sendNotificationIntent.putExtra(EXTRA_PRAYER_NAME, prayerName);
            sendNotificationIntent.putExtra(EXTRA_PRAYER_TIME, alarmTime);
            startWakefulService(context, sendNotificationIntent);
            //set the next prayer alarm
            addPrayerAlarm(context);
        }
    }



    public void addPrayerAlarm(Context context){
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context,PrayerAlarmReceiver.class);
        //get the next prayer
        next_prayer_time = getNextPrayer(context);
        Calendar cal = getCalendarFromPrayerTime(next_prayer_time.getTime(), next_prayer_time.getTomorrow());
        if(next_prayer_time.getName().equals(context.getString(R.string.fajr))){ //fajr of next day, bring prayers of next day and update database
            new LoadPrayersAsyncTask(context,cal).execute();
        }
        intent.putExtra(EXTRA_PRAYER_NAME,next_prayer_time.getName());
        intent.putExtra(EXTRA_PRAYER_TIME, cal.getTimeInMillis());
        alarmIntent = PendingIntent.getBroadcast(context, ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            //lollipop_mr1 is 22, this is only 23 and above
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), alarmIntent);
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //JB_MR2 is 18, this is only 19 and above.
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), alarmIntent);
        } else {
            //available since api1
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), alarmIntent);
        }

        Intent watchIntent = new Intent(context, UpdateWatchIntentService.class);
        watchIntent.putExtra(EXTRA_PRAYER_NAME,next_prayer_time.getName());
        watchIntent.putExtra(EXTRA_PRAYER_TIME,next_prayer_time.getTime());
        startWakefulService(context,watchIntent);

        // SET PASSIVE LOCATION RECEIVER
        Intent passiveIntent = new Intent(context, PassiveLocationChangeReceiver.class);
        PendingIntent locationListenerPassivePendingIntent = PendingIntent.getActivity(context, PASSIVE_LOCATION_ID, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        requestPassiveLocationUpdates(context, locationListenerPassivePendingIntent);

        // Enable {@code SampleBootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, PrayerOnBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }


    private Prayer getNextPrayer(Context context) {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        now.setTimeInMillis(System.currentTimeMillis());
        ArrayList<String> prayerTimes = Utility.getPrayTimes(now,context);
        int pos = 0;
        LocalTime nowLocal = LocalTime.now();
        Log.d("get current time", nowLocal.toString());
        LocalTime limit;
        for(int i=0; i<prayerTimes.size(); i++){
            limit = new LocalTime(prayerTimes.get(i));
            Boolean isLate = nowLocal.isAfter(limit);
            if(isLate)
                pos++;
        }
        //case pos is out of bound, get first prayer of tomorrow
        if(pos == prayerTimes.size() ){
            Calendar tomorrow= Calendar.getInstance(TimeZone.getDefault());
            tomorrow.setTimeInMillis(System.currentTimeMillis());
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            prayerTimes = Utility.getPrayTimes(tomorrow,context);
            saveNextPrayerPos(context,0);
            saveNextPrayerTime(context,prayerTimes.get(0));
            context.getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null); //to notifiy change of next prayer
            return new Prayer("Fajr",prayerTimes.get(0),true); //true: tomorrow
        }

        String name = Utility.getPrayerName(pos,context);
        saveNextPrayerPos(context,pos);
        saveNextPrayerTime(context,prayerTimes.get(pos));
        context.getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null);
        return new Prayer(name,prayerTimes.get(pos));
    }

    private void saveNextPrayerTime(Context context, String s) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.pref_next_prayer_time), s);
        editor.commit();
    }

    public void saveNextPrayerPos(Context context,int pos){
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(context.getString(R.string.pref_next_prayer), pos);
        editor.commit();
    }

    public Calendar getCalendarFromPrayerTime(String prayTime, boolean tomorrow){
        LocalTime time = new LocalTime(prayTime);
        Calendar cal= Calendar.getInstance(TimeZone.getDefault());
        Date date = time.toDateTimeToday(DateTimeZone.getDefault()).toDate();
        cal.setTime(date);
        if(tomorrow == true){
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal;
    }

    public void cancelAlarm(Context context) {
        if(alarmManager == null){
            alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        }
        if(alarmIntent == null){
            Intent intent = new Intent(context, PrayerAlarmReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(context,ALARM_ID,intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        alarmManager.cancel(alarmIntent);
        //REMOVE PASSIVE LOCATION RECEIVER
        Intent passiveIntent = new Intent(context, PassiveLocationChangeReceiver.class);
        PendingIntent locationListenerPassivePendingIntent = PendingIntent.getActivity(context, PASSIVE_LOCATION_ID, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        removePassiveLocationUpdates(context, locationListenerPassivePendingIntent);
        // Disable {@code SampleBootReceiver} so that it doesn't automatically restart the
        // alarm when the device is rebooted.
        ComponentName receiver = new ComponentName(context, PrayerOnBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }



    public void removePassiveLocationUpdates(Context context, PendingIntent pendingIntent) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.removeUpdates(pendingIntent);
        } catch (SecurityException se) {
            //do nothing. We should always have permision in order to reach this screen.
        }
    }

    public void requestPassiveLocationUpdates(Context context, PendingIntent pendingIntent) {
        long oneHourInMillis = 1000 * 60 * 60;
        long fiftyKinMeters = 50000;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                    oneHourInMillis, fiftyKinMeters, pendingIntent);
        } catch (SecurityException se) {
            Log.w("SetAlarmReceiver", se.getMessage(), se);
            //do nothing. We should always have permision in order to reach this screen.
        }
    }
}
