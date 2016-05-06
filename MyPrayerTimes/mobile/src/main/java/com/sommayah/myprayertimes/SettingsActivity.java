package com.sommayah.myprayertimes;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity  {
    protected final static int PLACE_PICKER_REQUEST = 9090;
    private ImageView mAttribution;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }  else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        // If we are using a PlacePicker location, we need to show attributions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mAttribution = new ImageView(this);
            mAttribution.setImageResource(R.drawable.powered_by_google_light);

            if (!Utility.isLocationLatLonAvailable(this)) {
                mAttribution.setVisibility(View.GONE);
            }

            setListFooter(mAttribution);
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // don't Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || WatchWidgetPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || LocationPreferenceFragment.class.getName().equals(fragmentName);
    }




    /**
     * This fragment shows calculation methode preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_calculation);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_calculation_methods_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_asr_calculation_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_dst_value)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_hijri_date_adj)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_language_list_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_high_alt_key)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_notification_ringtone_key)));

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WatchWidgetPreferenceFragment extends PreferenceFragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            ResultCallback<DataApi.DataItemResult>, SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";
        private static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";
        private final String TAG = "WatchWidgetPreference";

        private GoogleApiClient mGoogleApiClient;
        private String mPeerId;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_widget_watch);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            mPeerId = getActivity().getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
            bindPreferenceSummaryToValue(findPreference(getString(R.string.widget_pref_color_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.widget_pref_bg_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.watch_pref_bg_key)));
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            ComponentName name = getActivity().getIntent().getParcelableExtra(
                    WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);
            Log.d(TAG,"component name: "+ name);

        }

        @Override
        public void onStart() {
            super.onStart();
            mGoogleApiClient.connect();
        }

        @Override
        public void onStop() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            super.onStop();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + bundle);
            }

            /*if (mPeerId != null) {
                Uri.Builder builder = new Uri.Builder();
                Uri uri = builder.scheme("wear").path(PATH_WITH_FEATURE).authority(mPeerId).build();
                Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
            } else {
                displayNoConnectedDeviceDialog();
            }*/
        }

        @Override
        public void onConnectionSuspended(int i) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + i);
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + connectionResult);
            }
        }



        @Override
        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                DataItem configDataItem = dataItemResult.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                DataMap config = dataMapItem.getDataMap();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.watch_pref_bg_key), config.getString(Utility.WATCH_BG_COLOR));
                editor.commit();
            }
        }

        private void displayNoConnectedDeviceDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String messageText = getResources().getString(R.string.title_no_device_connected);
            String okText = getResources().getString(R.string.ok_no_device_connected);
            builder.setMessage(messageText)
                    .setCancelable(false)
                    .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) { }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        private void sendConfigUpdateMessage(final String configKey, final int color) {
            new AsyncTask<Void, Void, List<Node>>(){

                @Override
                protected List<Node> doInBackground(Void... params) {
                    return getNodes();
                }

                @Override
                protected void onPostExecute(List<Node> nodeList) {
                    for(Node node : nodeList) {
                        DataMap config = new DataMap();
                        config.putInt(configKey, color);
                        byte[] rawData = config.toByteArray();
                        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_WITH_FEATURE, rawData);

                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Sent watch face config message: " + configKey + " -> "
                                    + Integer.toHexString(color));
                        }

                        result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                Log.v(TAG, "Phone: " + sendMessageResult.getStatus().getStatusMessage());
                            }
                        });
                    }
                }
            }.execute();

        }

        private List<Node> getNodes() {
            List<Node> nodes = new ArrayList<Node>();
            NodeApi.GetConnectedNodesResult rawNodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : rawNodes.getNodes()) {
                nodes.add(node);
            }
            return nodes;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SharedPreferences prefs
                    = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if(key.equals(getString(R.string.watch_pref_bg_key))
                    ||key.equals(getString(R.string.pref_twentyfour_switch_key))
                    ||key.equals(getString(R.string.pref_show_hijri_switch_key))){
                String[] colorNames = getResources().getStringArray(R.array.color_array);
                int i = Integer.valueOf(prefs.getString(getString(R.string.watch_pref_bg_key),"0"));
                Boolean hijri = prefs.getBoolean(getString(R.string.pref_show_hijri_switch_key),true);
                Boolean twentyfour = prefs.getBoolean(getString(R.string.pref_twentyfour_switch_key), false);
                if (i >= 0 && i < 4)
                    if (mGoogleApiClient.isConnected()) {
                        // sendConfigUpdateMessage(KEY_BACKGROUND_COLOR, Color.parseColor(colorNames[i]));
                        Utility.sendPreferenceInfoToWatch(hijri,twentyfour,i,mGoogleApiClient);
                    }
            }
        }
    }

    /**
     * This fragment shows calculation methode preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class LocationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_location);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
          //  bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key_manual)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check to see if the result is from our Place Picker intent
        if (requestCode == PLACE_PICKER_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String address = place.getAddress().toString();
                LatLng latLong = place.getLatLng();
                Log.d("lat setting", latLong.toString());

                // If the provided place doesn't have an address, we'll form a display-friendly
                // string from the latlng values.
                if (TextUtils.isEmpty(address)) {
                    address = String.format("(%.2f, %.2f)",latLong.latitude, latLong.longitude);
                }

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_location_key), address);

                // Also store the latitude and longitude so that we can use these to get a precise
                // result from our prayer service.
                editor.putFloat(getString(R.string.pref_location_latitude),
                        (float) latLong.latitude);
                editor.putFloat(getString(R.string.pref_location_longitude),
                        (float) latLong.longitude);
                editor.commit();

                // Tell the SyncAdapter that we've changed the location, so that we can update
                // our UI with new values. We need to do this manually because we are responding
                // to the PlacePicker widget result here instead of allowing the
                // LocationEditTextPreference to handle these changes and invoke our callbacks.
                Preference locationPreference = findPreference(getString(R.string.pref_location_key));
                //  setPreferenceSummary(locationPreference, address);

                // Add attributions for our new PlacePicker location.
                if (mAttribution != null) {
                    mAttribution.setVisibility(View.VISIBLE);
                } else {
                    // For pre-Honeycomb devices, we cannot add a footer, so we will use a snackbar
                    View rootView = findViewById(android.R.id.content);
                    Snackbar.make(rootView, getString(R.string.attribution_text),
                            Snackbar.LENGTH_LONG).show();
                }

                //    Utility.resetLocationStatus(this);
                //    SunshineSyncAdapter.syncImmediately(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}
