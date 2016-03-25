package com.sommayah.myprayertimes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by sommayahsoliman on 3/21/16.
 */
/*how to create custom dialog with several return key,values
 http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes/4805325#4805325
 */
public class OffsetDialogPreference extends DialogPreference {

    private static final int DEFAULTVALUE = 0;
    private int mMinutes = 60;
    private String minutes;
    EditText editText1;
    EditText editText2;
    EditText editText3;
    EditText editText4;
    EditText editText5;


    public OffsetDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false); //to indicate to the super Preference class that you persist the preference value on your own.
        setSummary(getPrayerSummary());
        setDialogLayoutResource(R.layout.offset_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        //update the view with the value(s) of your preference
        // the view was created by my custom onCreateDialogView()
        editText1 = (EditText)view.findViewById(R.id.editText_fajr);
        editText2 = (EditText)view.findViewById(R.id.editText_dhuhr);
        editText3 = (EditText)view.findViewById(R.id.editText_asr);
        editText4 = (EditText)view.findViewById(R.id.editText_maghrib);
        editText5 = (EditText)view.findViewById(R.id.editText_isha);
        //get the saved value in this case
        editText1.setText(String.valueOf(DEFAULTVALUE));
        editText2.setText(String.valueOf(DEFAULTVALUE));
        editText3.setText(String.valueOf(DEFAULTVALUE));
        editText4.setText(String.valueOf(DEFAULTVALUE));
        editText5.setText(String.valueOf(DEFAULTVALUE));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        // if positiveResult is true then persist the value(s) from your view to the SharedPreferences.
        if(positiveResult == true){
            String[] prayerOffsets = {editText1.getText().toString(), editText2.getText().toString(),
                    editText3.getText().toString(), editText4.getText().toString(), editText5.getText().toString()};
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for(int i = 0; i<5 ;i++){ //five offsets
                String key = getContext().getString(R.string.pref_prayer_offsets)+String.valueOf(i);
                int offset = Integer.valueOf(prayerOffsets[i]);
                editor.putInt(key , offset);
                editor.commit();
            }
            setSummary(getPrayerSummary());
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        TextWatcher textWatcher = (new TextWatcher() {
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
                    // Check if the EditText is greater than 60
                    Editable s1 = editText1.getText();
                    Editable s2 = editText2.getText();
                    Editable s3 = editText3.getText();
                    Editable s4 = editText4.getText();
                    Editable s5 = editText5.getText();

                    boolean enable = is_valid(s1) && is_valid(s2) && is_valid(s3) && is_valid(s4) && is_valid(s5);

                    if (enable == false) {
                        // Disable OK button
                        positiveButton.setEnabled(false);
                    } else {
                        // Re-enable the button.
                        positiveButton.setEnabled(true);


                    }
                }
            }
        });
        editText1.addTextChangedListener(textWatcher);
        editText2.addTextChangedListener(textWatcher);
        editText3.addTextChangedListener(textWatcher);
        editText4.addTextChangedListener(textWatcher);
        editText5.addTextChangedListener(textWatcher);
    }



    private boolean is_valid(Editable s1) {
        try {
            int value = (s1 == null || s1.length() <= 0) ? 0 : Integer.valueOf(s1.toString());
            if (value <= 60)
                return true;
            else
                return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getPrayerSummary(){
        String summary = "[0, 0, 0, 0, 0]";
                PreferenceManager.getDefaultSharedPreferences(getContext());
        int[] offsetArray = Utility.getOffsetArray(getContext());
        summary = "[ "+ offsetArray[0] +" , " +  offsetArray[2] + " , " + offsetArray[3] + " , "
                + offsetArray[5] + " , " + offsetArray[6] + " ]";
        return summary;
    }
}
