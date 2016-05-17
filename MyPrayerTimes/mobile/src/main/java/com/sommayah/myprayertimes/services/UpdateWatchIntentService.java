package com.sommayah.myprayertimes.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class UpdateWatchIntentService extends IntentService implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    public GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    public static final String EXTRA_PRAYER_NAME = "prayer_name";
    public static final String EXTRA_PRAYER_TIME = "prayer_time";
    private String prayer_time;
    private String prayer_name;
    private String mdate = " ";
    private Intent mIntent;
    public UpdateWatchIntentService() {
        super("UpdateWatchIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        mIntent = intent;
        prayer_name = intent.getStringExtra(EXTRA_PRAYER_NAME);
        prayer_time = intent.getStringExtra(EXTRA_PRAYER_TIME);
        mdate = Utility.getSmallHijriDate(getApplicationContext());
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mResolvingError = false;
        Utility.sendPrayerInfoToWatch(getApplicationContext(),prayer_name, prayer_time, mdate, mGoogleApiClient);
        PrayerAlarmReceiver.completeWakefulIntent(mIntent);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("in watch service", "Connection to Google API client suspended");
        PrayerAlarmReceiver.completeWakefulIntent(mIntent);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
//            try {
//                mResolvingError = true;
//                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
//            } catch (IntentSender.SendIntentException e) {
//                // There was an error with the resolution intent. Try again.
//                mGoogleApiClient.connect();
//            }
        } else {
            Log.e("in watch service", "Connection to Google API client has failed");
            mResolvingError = false;
        }
        PrayerAlarmReceiver.completeWakefulIntent(mIntent); //we want to end service if not connected
    }
}
