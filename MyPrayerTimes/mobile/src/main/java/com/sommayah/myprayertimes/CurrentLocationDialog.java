package com.sommayah.myprayertimes;

import android.app.Activity;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by sommayahsoliman on 3/17/16.
 */
public class CurrentLocationDialog extends DialogPreference {
    public CurrentLocationDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Check to see if Google Play services is available. The Place Picker API is available
        // through Google Play services, so if this is false, we'll just carry on as though this
        // feature does not exist. If it is true, however, we can add a widget to our preference.
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode == ConnectionResult.SUCCESS) {
            // Add the get current location widget to our location preference
            setEnabled(true);
            setWidgetLayoutResource(R.layout.pref_current_location);
            String dialogmessage = Utility.getPreferredLocation(context);
            if(dialogmessage.equals("")){
                dialogmessage = context.getString(R.string.press_on_circle);
            }
            setDialogMessage(dialogmessage);
        }else{
            setDialogMessage(context.getString(R.string.automatic_location_disabled));
        }

        setSummary(Utility.getPreferredLocation(context));

    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        View currentLocation = view.findViewById(R.id.current_location);
        if(currentLocation!= null) { //there is a bug that it can be null in some case
            currentLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = getContext();

                    // Launch the Place Picker so that the user can specify their location, and then
                    // return the result to SettingsActivity.
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();


                    // We are in a view right now, not an activity. So we need to get ourselves
                    // an activity that we can use to start our Place Picker intent. By using
                    // SettingsActivity in this way, we can ensure the result of the Place Picker
                    // intent comes to the right place for us to process it.
                    Activity settingsActivity = (SettingsActivity) context;
                    try {
                        settingsActivity.startActivityForResult(
                                builder.build(settingsActivity), SettingsActivity.PLACE_PICKER_REQUEST);

                    } catch (GooglePlayServicesNotAvailableException
                            | GooglePlayServicesRepairableException e) {
                        // What did you do?? This is why we check Google Play services in onResume!!!
                        // The difference in these exception types is the difference between pausing
                        // for a moment to prompt the user to update/install/enable Play services vs
                        // complete and utter failure.
                        // If you prefer to manage Google Play services dynamically, then you can do so
                        // by responding to these exceptions in the right moment. But I prefer a cleaner
                        // user experience, which is why you check all of this when the app resumes,
                        // and then disable/enable features based on that availability.
                    }
                }
            });
        }
        return view;
    }



    @Override
    protected void onClick() {
        // Check to see if Google Play services is available. The Place Picker API is available
        // through Google Play services, so if this is false, we'll just carry on as though this
        // feature does not exist. If it is true, however, we can add a widget to our preference.
        // Add the get current location widget to our location preference

        super.onClick();
    }


}
