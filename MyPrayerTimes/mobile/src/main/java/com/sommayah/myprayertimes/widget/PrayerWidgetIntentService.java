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

import java.util.ArrayList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PrayerWidgetIntentService extends IntentService {
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
                ArrayList<String> prayTimes = new ArrayList<>();
                do{
                    prayTimes.add(data.getString(COL_PRAYER_TIME));
                }while (data.moveToNext());
                int nextPrayer = Utility.getNextPos(getApplicationContext());
                if (Utility.getPreferredTimeFormat(getApplicationContext()) == PrayTime.TIME12) { //12 hr or 24 formate{
                    //update if 12 format
                    for (int i = 0; i < prayTimes.size(); i++) {
                        LocalTime time = new LocalTime(prayTimes.get(i));
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("h:mm");
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
                    int nextcolor = Utility.getWidgetTextColor(getApplicationContext());
                    if(i == nextPrayer){
                        views.setInt(temp, "setTextColor", getResources().getColor(nextcolor));
                        views.setInt(tempName, "setTextColor", getResources().getColor(nextcolor));
                    }else{
                        views.setInt(temp, "setTextColor", getResources().getColor(android.R.color.secondary_text_dark));
                        views.setInt(tempName, "setTextColor", getResources().getColor(android.R.color.secondary_text_dark));
                    }

                }
                int transparencyPercent = Utility.getTransparencyPercent(getApplicationContext());
                int transcolor = Utility.getWidgetTransparencyColor(getApplicationContext());
                if(transcolor == android.R.color.transparent){
                    views.setInt(R.id.widget, "setBackgroundColor", getResources().getColor(transcolor));
                }else{
                    int colorvalue = Color.parseColor(Utility.getTransparencyNumber(transparencyPercent, transcolor));
                    views.setInt(R.id.widget, "setBackgroundColor", colorvalue);
                }

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
