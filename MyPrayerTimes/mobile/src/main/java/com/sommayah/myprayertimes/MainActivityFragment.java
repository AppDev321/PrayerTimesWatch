package com.sommayah.myprayertimes;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;
import com.sommayah.myprayertimes.data.PrayerContract;

import java.util.Calendar;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Bind(R.id.recyclerview_prayer) RecyclerView mRecyclerView;
    @Bind(R.id.recyclerview_prayer_empty) View mEmptyView;

    private PrayerAdapter mAdapter;
    private static final int PRAYER_LOADER = 0;
    // Specify the columns we need.
    private static final String[] PRAYER_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            PrayerContract.PrayerEntry.COLUMN_PRAYERNAME,
            PrayerContract.PrayerEntry.COLUMN_PRAYERTIME,
    };

    // These indices are tied to PRAYER_COLUMNS.  If PRAYER_COLUMNS changes, these
    // must change.
    static final int COL_PRAYER_NAME = 0;
    static final int COL_PRAYER_TIME = 1;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                onChangedSettings(prefs, key);

            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);

        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this,rootView);

        if(getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),2,LinearLayoutManager.VERTICAL,false));
        }else {
            // Set the layout manager
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

//        if(!Utility.isAlarmInitiated(getContext()) && Utility.isLocationLatLonAvailable(getContext())){
//            SharedPreferences sharedPreferences =
//                    PreferenceManager.getDefaultSharedPreferences(getContext());
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putBoolean(getString(R.string.pref_alarm_initiated), true);
//            editor.commit();
//            PrayerAlarmReceiver prayerAlarmReceiver = new PrayerAlarmReceiver();
//            prayerAlarmReceiver.addPrayerAlarm(getContext());
//
//        }

        mAdapter = new PrayerAdapter(getActivity(), mEmptyView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(PRAYER_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged() {
        getLoaderManager().restartLoader(PRAYER_LOADER, null, this);
    }

    @Override
    public void onDestroyView() {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null); //update ui, next prayer time remaining
    }

    @Override
    public void onPause() {
        super.onPause();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void onChangedSettings(SharedPreferences prefs, String key){

        if (key.equals(getString(R.string.pref_calculation_methods_key))
                || key.equals(getString(R.string.pref_asr_calculation_key))
                || key.equals(getString(R.string.pref_dst_value))
                || key.equals(getString(R.string.pref_high_alt_switch))
                || key.equals(getString(R.string.pref_time_format_key))
                || key.equals(getString(R.string.pref_loc_manual_set)) //incase we use the saved location
                || key.equals(getString(R.string.pref_location_key_manual))
                || key.equals(getString(R.string.pref_location_status_key))
                || isKeyOffset(key)) {
            if(!key.equals(getString(R.string.pref_time_format_key)) && !key.equals(getString(R.string.pref_location_status_key))){
                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                PrayerAlarmReceiver alarm = new PrayerAlarmReceiver();
                new LoadPrayersAsyncTask(getContext(),cal).execute();
                //restart the alarm for next prayer based on new times
                alarm.cancelAlarm(getContext());
                alarm.addPrayerAlarm(getContext());

            }
            if(key.equals(getString(R.string.pref_location_status_key))){
                updateEmptyView();
            }
            //update ui
            if (mAdapter != null) {
                getContext().getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null);
                Utility.updateWidgets(getContext());
            }
        }

        //no need to listen to hijri date adjustment because we display the new one onresume
        if(key.equals(getString(R.string.pref_widget_transparency_key))
                || key.equals(getString(R.string.widget_pref_bg_key))
                || key.equals(getString(R.string.widget_pref_color_key))){
            Utility.updateWidgets(getContext());
        }

        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }

    private boolean isKeyOffset(String key) {
            if(key.equals(getString(R.string.pref_prayer_offsets) + "0")
                    || key.equals(getString(R.string.pref_prayer_offsets) + "1")
                    || key.equals(getString(R.string.pref_prayer_offsets) + "2")
                    || key.equals(getString(R.string.pref_prayer_offsets) + "3")
                    || key.equals(getString(R.string.pref_prayer_offsets) + "4")
                    ){
                return true;
            }else
                return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri prayerUri = PrayerContract.PrayerEntry.CONTENT_URI;
        return new CursorLoader(getActivity(),
                prayerUri,
                PRAYER_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null)
            mAdapter.swapCursor(data);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if ( mAdapter.getItemCount() == 0 ) {
            TextView tv = (TextView) mEmptyView;
            if ( null != tv ) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_prayer_list;
                @Utility.LocationStatus int location = Utility.getLocationStatus(getActivity());
                switch (location) {
                    case Utility.LOCATION_STATUS_PERMISSION_DENIED:
                        message = R.string.permission_denied;
                        break;
                    case Utility.LOCATION_STATUS_INVALID:
                        message = R.string.invalid_location;
                        break;
                    case Utility.LOCATION_STATUS_DISABLED:
                        message=R.string.disabled_location;
                        break;
                    default:
                        message = R.string.empty_prayer_list;

                }
                tv.setText(message);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),2,LinearLayoutManager.VERTICAL,false));
        }else {
            // Set the layout manager
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
