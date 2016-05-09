/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

public class LocationEditTextPreference extends EditTextPreference {
    static final private int DEFAULT_MINIMUM_LOCATION_LENGTH = 2;
    private int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.LocationEditTextPreference,
                0, 0);
        try {
            mMinLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MINIMUM_LOCATION_LENGTH);
        } finally {
            a.recycle();
        }
        String summary = Utility.getManualLocation(context);
        if(summary.equals("")){
            summary = context.getString(R.string.location_not_valid);
        }

        setSummary(summary);

    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        EditText et = getEditText();
        et.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Dialog d = getDialog();
                if (d instanceof AlertDialog) {
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    // Check if the EditText is empty
                    if (s.length() < mMinLength) {
                        // Disable OK button
                        positiveButton.setEnabled(false);
                    } else {
                        // Re-enable the button.
                        positiveButton.setEnabled(true);
                    }
                }
            }
        });
    }



    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.d("location after dismiss", Utility.getPreferredLocation(getContext()));
        EditText et = getEditText();
        Log.d("location after dismiss", et.getText().toString());
        boolean valid = validateLocation(et.getText().toString());
        if(valid == false){
            et.setText("");
        }
        super.onDialogClosed(positiveResult);
    }

    private boolean validateLocation(String address){
        Geocoder geoCoder = new Geocoder(getContext());
        if (address != null) {
            try {
                List<Address> addressList = geoCoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    double lat = addressList.get(0).getLatitude();
                    double lng = addressList.get(0).getLongitude();
                    addManualLocation(lat,lng);
                    setSummary(address);
                    Utility.setLocationStatus(getContext(),Utility.LOCATION_STATUS_OK);
                    return true;
                }else{
                    setSummary(getContext().getString(R.string.location_not_valid));
                    addManualLocation(Utility.DEFAULT_LATLONG,Utility.DEFAULT_LATLONG);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } // end catch
        } // end if
        return false;

    }

    private void addManualLocation(double lat, double lng) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        // Also store the latitude and longitude so that we can use these to get a precise
        // result from our prayer service.
        editor.putFloat(getContext().getString(R.string.pref_location_latitude_manual),
                (float) lat);
        editor.putFloat(getContext().getString(R.string.pref_location_longitude_manual),
                (float) lng);
        editor.commit();
    }


}
