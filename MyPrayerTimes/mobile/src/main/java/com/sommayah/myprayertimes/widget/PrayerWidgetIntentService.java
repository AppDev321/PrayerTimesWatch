package com.sommayah.myprayertimes.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sommayah.myprayertimes.MainActivity;
import com.sommayah.myprayertimes.R;
import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.dataModels.PrayTime;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PrayerWidgetIntentService extends IntentService {


    public PrayerWidgetIntentService() {
        super("PrayerWidgetIntentService");
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                    PrayerWidget.class));

            // Perform this loop procedure for each Today widget
            for (int appWidgetId : appWidgetIds) {
                int layoutId = R.layout.prayer_widget;
                RemoteViews views = new RemoteViews(getPackageName(), layoutId);
                ArrayList<String> prayTimes;
                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                prayTimes = Utility.getPrayTimes(cal, getApplicationContext());
                int nextPrayer = Utility.getNextPos(prayTimes);
                if (Utility.getPreferredTimeFormat(getApplicationContext()) == PrayTime.TIME12) { //12 hr or 24 formate{
                    //update if 12 format
                    for (int i = 0; i < prayTimes.size(); i++) {
                        LocalTime time = new LocalTime(prayTimes.get(i));
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("hh:mm");
                        String str = fmt.print(time);
                        prayTimes.set(i, str);

                    }
                }
                // Add the data to the RemoteViews
                views.setTextViewText(R.id.appwidget_date, Utility.getHijriDate(getApplicationContext()));
                int temp, tempName;
                String[] id = new String[]{"textViewFajrTime", "textViewSunRiseTime", "textViewDhuhrTime", "textViewAsrTime"
                , "textViewMaghribTime", "textViewIshaTime"};
                String[] namesId = new String[]{"textViewFajr", "textViewSunRise", "textViewDhuhr", "textViewAsr"
                        , "textViewMaghrib", "textViewIsha"};


                //code from: http://stackoverflow.com/questions/31623126/how-to-put-textviews-in-an-array-and-findviewbyid-of-them
                for(int i=0; i<id.length; i++){
                    temp = getResources().getIdentifier(id[i], "id", getPackageName());
                    tempName = getResources().getIdentifier(namesId[i], "id", getPackageName());
                    views.setTextViewText(temp, prayTimes.get(i));
                    if(i == nextPrayer){
                        views.setInt(temp, "setTextColor", getResources().getColor(android.R.color.white));
                        views.setInt(tempName, "setTextColor", getResources().getColor(android.R.color.white));
                    }else{
                        views.setInt(temp, "setTextColor", getResources().getColor(android.R.color.secondary_text_dark));
                        views.setInt(tempName, "setTextColor", getResources().getColor(android.R.color.secondary_text_dark));
                    }

                }

//                views.setTextViewText(R.id.textViewFajrTime, prayTimes.get(0));
//                views.setTextViewText(R.id.textViewSunRiseTime, prayTimes.get(1));
//                views.setTextViewText(R.id.textViewDhuhrTime, prayTimes.get(2));
//                views.setTextViewText(R.id.textViewAsrTime, prayTimes.get(3));
//                views.setTextViewText(R.id.textViewMaghribTime, prayTimes.get(4));
//                views.setTextViewText(R.id.textViewIshaTime, prayTimes.get(5));


                // Create an Intent to launch MainActivity
                Intent launchIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
                views.setOnClickPendingIntent(R.id.widget, pendingIntent);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }



}
