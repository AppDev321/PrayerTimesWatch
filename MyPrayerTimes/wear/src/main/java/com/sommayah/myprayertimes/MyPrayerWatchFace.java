/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sommayah.myprayertimes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyPrayerWatchFace extends CanvasWatchFaceService {
    public static final String PRAYER_PATH = "/prayer";
    public static final String PRAYER_NAME_KEY = "prayername";
    public static final String PRAYER_TIME_KEY = "prayertime";
    private static final String HIJRI_DATE_KEY = "hijridate";
    public static final String PREF_PATH = "/pref";
    public static final String HIJRI_KEY = "hijridate";
    public static final String TWENTYFOUR_KEY = "timeformat";
    public static final String WATCH_BG_COLOR = "bgcolor";
    public static final String ACTION_RECEIVE = "com.sommayah.myprayertimes.watchface.DATA";
    public static final String TAG = "MyPrayerWatchFace";
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyPrayerWatchFace.Engine> mWeakReference;

        public EngineHandler(MyPrayerWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyPrayerWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mNamePaint;
        Paint mTimePaint;
        Paint mDatePaint;
        Paint mLinePaint;
        boolean mAmbient;
        float mLineHeight;
        Time mTime;
        GoogleApiClient mGoogleApiClient;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        float mXOffset;
        float mYOffset;
        float mXSmallOffset;

        int mInteractiveBackgroundColor =
                DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND;
        int mInteractiveHourDigitsColor =
                DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS; //use hour color for all time color

        String prayer_name = "";
        String prayer_time = "";
        String prayer_date = "";
        boolean showHijri = true;
        boolean format24 = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyPrayerWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyPrayerWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mTextPaint = new Paint();
            mTextPaint = createTextPaint(mInteractiveHourDigitsColor);
           // mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));
            mNamePaint = new Paint();
            //mNamePaint = createTextPaint(resources.getColor(R.color.digital_text));
            mNamePaint = createTextPaint(mInteractiveHourDigitsColor);
            mTimePaint = new Paint();
            //mTimePaint = createTextPaint(resources.getColor(R.color.digital_text));
            mTimePaint = createTextPaint(mInteractiveHourDigitsColor);
            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.colorAccent));
            mTime = new Time();
            mLinePaint = new Paint();
            mLinePaint.setColor(resources.getColor(R.color.line_color));
            mLinePaint.setTextAlign(Paint.Align.CENTER);
            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mGoogleApiClient = new GoogleApiClient.Builder(MyPrayerWatchFace.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyPrayerWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyPrayerWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyPrayerWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_size_round : R.dimen.digital_date_size);
            float nameTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_name_size_round : R.dimen.digital_name_size);
            float timeSize = resources.getDimension(isRound
                    ? R.dimen.digital_time_size_round : R.dimen.digital_time_size);
            mXSmallOffset =  resources.getDimension(R.dimen.digital_x_offset_round_7dp);

            mTextPaint.setTextSize(textSize);
            mDatePaint.setTextSize(dateTextSize);
            mDatePaint.setTextAlign(Paint.Align.CENTER);
            mDatePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            mNamePaint.setTextSize(nameTextSize);
            mNamePaint.setTextAlign(Paint.Align.CENTER);
            mTimePaint.setTextSize(timeSize);
            mNamePaint.setTextAlign(Paint.Align.CENTER);

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            adjustPaintColorToCurrentMode(mBackgroundPaint, mInteractiveBackgroundColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
            adjustPaintColorToCurrentMode(mTextPaint, mInteractiveHourDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            //currently the prayer times are same color is the current time
            adjustPaintColorToCurrentMode(mNamePaint, mInteractiveHourDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            adjustPaintColorToCurrentMode(mTimePaint, mInteractiveHourDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mNamePaint.setAntiAlias(!inAmbientMode);
                    mDatePaint.setAntiAlias(!inAmbientMode);
                    mTimePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                                                   int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }



        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyPrayerWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
//                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
//                            R.color.background : R.color.colorPrimaryDark));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                Shader shader = new LinearGradient(0, 0, 0, bounds.height(),
                        getResources().getColor(R.color.colorPrimaryDark),
                        getResources().getColor(R.color.colorPrimary)
                        , Shader.TileMode.MIRROR);
               // mBackgroundPaint.setShader(shader);
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            mTime.format("%k:%M:%S");
            int hour = mTime.hour;
            if(format24 == false){
                hour = mTime.hour%12;
                if(hour == 0) hour = 12;
            }

            String text = mAmbient
                    ? String.format("%d:%02d", hour, mTime.minute)
                    :String.format("%02d:%02d:%02d", hour, mTime.minute, mTime.second);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            if(!isInAmbientMode()) {
                if (!prayer_date.equals("") && showHijri == true) {
                    canvas.drawText(prayer_date,
                            bounds.width() / 3 - mXSmallOffset, mYOffset + 0.6f*  mLineHeight, mDatePaint);
                }
            }
            if (getPeekCardPosition().isEmpty()) {
                if (!prayer_name.equals("")) {
                    canvas.drawText(prayer_name,
                            bounds.width() / 3 - mXSmallOffset, mYOffset + mLineHeight * 1.25f, mNamePaint);
                }

                if (!prayer_time.equals("")) {
                    String showtime = prayer_time;
                    if(format24 == false){
                        LocalTime time = new LocalTime(prayer_time);
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("h:mm aa");
                        showtime =fmt.print(time);
                    }
                    canvas.drawText(showtime, bounds.width() * 1 / 2 - mXSmallOffset, mYOffset + 1.25f * mLineHeight, mTimePaint);
                }


            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void updateConfigDataItemAndUiOnStartup() {
            DigitalWatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new DigitalWatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            DigitalWatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

                            updateUiForConfigDataMap(startupConfig);
                        }
                    }
            );
        }


        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
            addIntKeyIfMissing(config, DigitalWatchFaceUtil.KEY_HOURS_COLOR,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
        }

        private void addIntKeyIfMissing(DataMap config, String key, int color) {
            if (!config.containsKey(key)) {
                config.putInt(key, color);
            }
        }

        private void updateUiForConfigDataMap(final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) {
                    continue;
                }
                int color = config.getInt(configKey);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found watch face config key: " + configKey + " -> "
                            + Integer.toHexString(color));
                }
                if (updateUiForKey(configKey, color)) {
                    uiUpdated = true;
                }
            }
            if (uiUpdated) {
                invalidate();
            }
        }
        private void updatePaintIfInteractive(Paint paint, int interactiveColor) {
            if (!isInAmbientMode() && paint != null) {
                paint.setColor(interactiveColor);
            }
        }

        private void setInteractiveBackgroundColor(int color) {
            mInteractiveBackgroundColor = color;
            if(color == Color.WHITE){ //change the text to the other color(green) so that it shows
                setInteractiveHourDigitsColor(getColor(R.color.colorPrimaryDark));
            }
            updatePaintIfInteractive(mBackgroundPaint, color);
        }

        private void setInteractiveHourDigitsColor(int color) {
            mInteractiveHourDigitsColor = color;
            updatePaintIfInteractive(mTextPaint, color);
            updatePaintIfInteractive(mNamePaint, color);
            updatePaintIfInteractive(mTimePaint, color);
        }

        /**
         * Updates the color of a UI item according to the given {@code configKey}. Does nothing if
         * {@code configKey} isn't recognized.
         *
         * @return whether UI has been updated
         */
        private boolean updateUiForKey(String configKey, int color) {
            if (configKey.equals(DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR)) {
                setInteractiveBackgroundColor(color);
            } else if (configKey.equals(DigitalWatchFaceUtil.KEY_HOURS_COLOR)) {
                setInteractiveHourDigitsColor(color);
            } else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
                return false;
            }
            return true;
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            // if (Log.isLoggable(TAG, Log.DEBUG)) {
            //    Log.d(TAG, "onConnected: " + connectionHint);
            // }
            Log.d(TAG, "onConnected: ");
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup();
            updateUiOnStartup();
        }

        private void updateUiOnStartup() {
            new GetDataTask().execute();

        }


        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "onDataChanged(): " + dataEvents);

            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    String path = event.getDataItem().getUri().getPath();
                    if(event.getDataItem() != null) {
                        if (PRAYER_PATH.equals(path)) {
                            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                            prayer_name = dataMapItem.getDataMap()
                                    .getString(PRAYER_NAME_KEY);
                            prayer_date = dataMapItem.getDataMap()
                                    .getString(HIJRI_DATE_KEY);
                            prayer_time = dataMapItem.getDataMap()
                                    .getString(PRAYER_TIME_KEY);
                        } else if (DigitalWatchFaceUtil.PATH_WITH_FEATURE.equals(path)) {
                            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                            DataMap config = dataMapItem.getDataMap();
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Config DataItem updated:" + config);
                            }
                            updateUiForConfigDataMap(config);
                        } else if (PREF_PATH.equals(path)) {
                            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                            showHijri = dataMapItem.getDataMap()
                                    .getBoolean(HIJRI_KEY);
                            format24 = dataMapItem.getDataMap()
                                    .getBoolean(TWENTYFOUR_KEY);
//                        byte[] rawData = dataMapItem.getDataMap()
//                                .getByteArray(WATCH_BG_COLOR);
//                        DataMap configKeysToOverwrite = DataMap.fromByteArray(rawData);
//                        DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
                        }
                    }
                    else {
                        Log.d(TAG, "Unrecognized path: " + path);
                    }

                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d("DataItem Deleted", event.getDataItem().toString());
                } else {
                    Log.d("Unknown data event type", "Type = " + event.getType());
                }
            }
        }

        private Collection<String> getNodes() {
            HashSet<String> results = new HashSet<>();
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }

            return results;
        }

        private String getRemoteNodeId() {
            HashSet<String> results = new HashSet<String>();
            NodeApi.GetConnectedNodesResult nodesResult =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            List<Node> nodes = nodesResult.getNodes();
            if (nodes.size() > 0) {
                return nodes.get(0).getId();
            }
            return null;
        }


        private class GetDataTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                Log.d(TAG, "in background task");
                String node = getRemoteNodeId();
                if (node != null) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(PRAYER_PATH)
                            .authority(node)
                            .build();

                    DataApi.DataItemResult result = Wearable.DataApi.getDataItem(mGoogleApiClient, uri).await();
                    if (result != null && null!= result.getDataItem()) {
                        String path = uri.getPath();
                        if (PRAYER_PATH.equals(path)) {
                            DataMapItem dataItem = DataMapItem.fromDataItem(result.getDataItem());
                            prayer_name = dataItem.getDataMap()
                                    .getString(PRAYER_NAME_KEY);
                            prayer_date = dataItem.getDataMap()
                                    .getString(HIJRI_DATE_KEY);
                            prayer_time = dataItem.getDataMap()
                                    .getString(PRAYER_TIME_KEY);
                        } else if (DigitalWatchFaceUtil.PATH_WITH_FEATURE.equals(path)) {
                            DataMapItem dataItem = DataMapItem.fromDataItem(result.getDataItem());
                            DataMap config = dataItem.getDataMap();
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Config DataItem updated:" + config);
                            }
                            updateUiForConfigDataMap(config);
                        } else if (PREF_PATH.equals(path)) {
                            DataMapItem dataItem = DataMapItem.fromDataItem(result.getDataItem());
                            showHijri = dataItem.getDataMap()
                                    .getBoolean(HIJRI_KEY);
                            format24 = dataItem.getDataMap()
                                    .getBoolean(TWENTYFOUR_KEY);
                        } else {
                            Log.d(TAG, "Unrecognized path: " + path);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }

    }
}
