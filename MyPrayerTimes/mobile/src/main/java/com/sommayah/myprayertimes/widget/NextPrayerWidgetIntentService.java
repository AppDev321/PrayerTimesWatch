package com.sommayah.myprayertimes.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;

import com.sommayah.myprayertimes.MainActivity;
import com.sommayah.myprayertimes.R;
import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.data.PrayerContract;
import com.sommayah.myprayertimes.dataModels.PrayTime;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NextPrayerWidgetIntentService extends IntentService {

    // Specify the columns we need.
    private static final String[] PRAYER_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            PrayerContract.PrayerEntry.COLUMN_PRAYERNAME,
            PrayerContract.PrayerEntry.COLUMN_PRAYERTIME,
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_PRAYER_NAME = 0;
    static final int COL_PRAYER_TIME = 1;

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
                Uri prayerUri = PrayerContract.PrayerEntry.CONTENT_URI;
                Cursor data = getContentResolver().query(prayerUri, PRAYER_COLUMNS, null,
                        null, null);
                if (data == null) {
                    return;
                }
                if (!data.moveToFirst()) {
                    data.close();
                    return;
                }
                int nextPrayer = Utility.getNextPos(getApplicationContext());
//                ArrayList<String> prayTimes = new ArrayList<>();
//                do{
//                    prayTimes.add(data.getString(COL_PRAYER_TIME));
//                }while (data.moveToNext());
                data.moveToPosition(nextPrayer);
                String prayName = data.getString(COL_PRAYER_NAME);
                String prayTime = data.getString(COL_PRAYER_TIME);

                if (Utility.getPreferredTimeFormat(getApplicationContext()) == PrayTime.TIME12) { //12 hr or 24 formate{
                        LocalTime time = new LocalTime(prayTime);
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("h:mm aa");
                        String str = fmt.print(time);
                        prayTime = str;
                }
                // Add the data to the RemoteViews
                views.setTextViewText(R.id.appwidget_date, Utility.getSmallHijriDate(getApplicationContext()));
                views.setTextViewText(R.id.textViewPrayerName, prayName);
                views.setTextViewText(R.id.textViewPrayerTime, prayTime);

                int color = Utility.getWidgetTextColor(getApplicationContext());
                int transparencyPercent = Utility.getTransparencyPercent(getApplicationContext());
                int transcolor = Utility.getWidgetTransparencyColor(getApplicationContext());
                if(transcolor == android.R.color.transparent){
                    views.setInt(R.id.widgetNextPrayer, "setBackgroundColor", getResources().getColor(transcolor));
                }else{
                    int colorvalue = Color.parseColor(Utility.getTransparencyNumber(transparencyPercent, transcolor));
                    views.setInt(R.id.widgetNextPrayer, "setBackgroundColor", colorvalue);
                }
                views.setInt(R.id.textViewPrayerTime, "setTextColor", getResources().getColor(color));

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
