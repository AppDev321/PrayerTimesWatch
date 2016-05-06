package com.sommayah.myprayertimes.services;

import android.app.IntentService;
import android.content.Intent;

import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;
import com.sommayah.myprayertimes.data.PrayerContract;

import java.util.ArrayList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class LoadPrayerToDBIntentService extends IntentService {
    public static final String EXTRA_PRAYER_ARRAY = "prayer_array";

    public LoadPrayerToDBIntentService() {
        super("LoadPrayerToDBIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            ArrayList<String> prayersArray = new ArrayList<>();
            prayersArray = intent.getStringArrayListExtra(EXTRA_PRAYER_ARRAY);
            if(prayersArray!= null ||prayersArray.size() != 0){
                Utility.addPrayersToDB(getApplicationContext(), prayersArray);
                getApplicationContext().getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null);
            }
        }
        PrayerAlarmReceiver.completeWakefulIntent(intent);
    }


}
