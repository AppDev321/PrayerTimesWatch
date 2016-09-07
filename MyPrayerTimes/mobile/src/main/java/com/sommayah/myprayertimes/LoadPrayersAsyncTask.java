package com.sommayah.myprayertimes;

import android.content.Context;
import android.os.AsyncTask;

import com.sommayah.myprayertimes.data.PrayerContract;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by sommayahsoliman on 4/23/16.
 */
public class LoadPrayersAsyncTask extends AsyncTask<Void, Void, Void> {
    Context context;
    Calendar cal;

    public LoadPrayersAsyncTask(Context context, Calendar cal){
        this.context = context;
        this.cal = cal;
    }

    @Override
    protected Void doInBackground(Void... params) {
        ArrayList<String> prayerTimes = new ArrayList<>();
        prayerTimes = Utility.getPrayTimes(cal, context);
        Utility.addPrayersToDB(context, prayerTimes);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        context.getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null);
        super.onPostExecute(aVoid);
    }

}