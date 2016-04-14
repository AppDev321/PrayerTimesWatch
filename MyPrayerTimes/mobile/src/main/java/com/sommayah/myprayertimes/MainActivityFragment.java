package com.sommayah.myprayertimes;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;
import com.sommayah.myprayertimes.data.PrayerContract;
import com.sommayah.myprayertimes.dataModels.PrayTime;

import java.util.ArrayList;
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
    private boolean mUseNextPrayerLayout;
    private PrayTime mPraytime;
    private PrayerAdapter mAdapter;
    private ArrayList<String> mPrayerTimes;
    private View mEmptyView;

    private static final int PRAYER_LOADER = 0;
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

        // Set the layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmptyView = rootView.findViewById(R.id.recyclerview_prayer_empty);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);



        if(!Utility.isAlarmInitiated(getContext())){
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.pref_alarm_initiated), true);
            editor.commit();
            PrayerAlarmReceiver prayerAlarmReceiver = new PrayerAlarmReceiver();
            prayerAlarmReceiver.addPrayerAlarm(getContext());

        }

        mAdapter = new PrayerAdapter(getActivity(), mEmptyView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

//        // The PrayerAdapter will take data from a source and
//        // use it to populate the RecyclerView it's attached to.
//        mPrayerAdapter = new PrayerAdapter(getActivity(), new PrayerAdapter.PrayerAdapterOnClickHandler() {
//            @Override
//            public void onClick(int prayerPos, PrayerAdapter.PrayerAdapterViewHolder vh) {
//                //leave it empty now till i impelement the click action
//            }
//        }, emptyView);
//
//        // specify an adapter (see also next example)
//        mRecyclerView.setAdapter(mPrayerAdapter);
////        if (savedInstanceState != null) {
////            mPrayerAdapterAdapter.onRestoreInstanceState(savedInstanceState);
////        }



        //ss:havent set the boolean value yet
       // mPrayerAdapter.setUseNextPrayerLayout(mUseNextPrayerLayout);


        return rootView;
    }

    @Override
    public void onResume() {
        //so that the next prayer is updated
        //update ui
//        if (mAdapter != null) {
//            mAdapter.clear();
//            mAdapter.add(mPrayerTimes);
//            mAdapter.notifyDataSetChanged();
//        }
        super.onResume();
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void onChangedSettings(SharedPreferences prefs, String key){

        //ss: so badly implemented has to change this if statement
        if (!key.equals(getString(R.string.pref_time_format_key)) && !key.equals(getString(R.string.pref_location_latitude))
        && !key.equals(getString(R.string.pref_location_longitude))) { //in case of format we dont need to grab data again
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            mPrayerTimes = Utility.getPrayTimes(cal, getContext());
            Utility.addPrayersToDB(getContext(), mPrayerTimes);

        }
        //update ui
        if (mAdapter != null) {
            getContext().getContentResolver().notifyChange(PrayerContract.PrayerEntry.CONTENT_URI,null);
            Utility.updateWidgets(getContext());
        }
        //no need to listen to hijri date adjustment because we display the new one onresume
        if(key.equals(getString(R.string.pref_widget_transparency_key))
                || key.equals(getString(R.string.widget_pref_bg_key))
                || key.equals(getString(R.string.widget_pref_color_key))){
            Utility.updateWidgets(getContext());
        }

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
        //implement it later
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
