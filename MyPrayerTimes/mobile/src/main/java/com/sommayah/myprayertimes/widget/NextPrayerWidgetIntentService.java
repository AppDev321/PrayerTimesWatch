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
public class NextPrayerWidgetIntentService extends IntentService {

    public NextPrayerWidgetIntentService() {
        super("NextPrayerWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                    NextPrayerWidget.class));

            // Perform this loop procedure for each Today widget
            for (int appWidgetId : appWidgetIds) {
                int layoutId = R.layout.next_prayer_widget;
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
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("h:mm aa");
                        String str = fmt.print(time);
                        prayTimes.set(i, str);

                    }
                }
                // Add the data to the RemoteViews
                views.setTextViewText(R.id.appwidget_date, Utility.getSmallHijriDate(getApplicationContext()));
                views.setTextViewText(R.id.textViewPrayerName, Utility.getPrayerName(nextPrayer,getApplicationContext()));
                views.setTextViewText(R.id.textViewPrayerTime, prayTimes.get(nextPrayer));

                // Create an Intent to launch MainActivity
                Intent launchIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
                views.setOnClickPendingIntent(R.id.widgetNextPrayer, pendingIntent);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }


}
