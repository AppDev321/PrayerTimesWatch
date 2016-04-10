package com.sommayah.myprayertimes.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sommayah.myprayertimes.services.PrayerNotificationService;

/**
 * Implementation of App Widget functionality.
 */
public class NextPrayerWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, NextPrayerWidgetIntentService.class));
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, NextPrayerWidgetIntentService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (PrayerNotificationService.ACTION_NEXT_PRAYER_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, NextPrayerWidgetIntentService.class));
        }
    }
}

