

package com.sommayah.myprayertimes;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

/* got the code from: http://www.javacodegeeks.com/2013/09/android-compass-code-example.html */

public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    // define the display assembly compass picture
    @Bind(R.id.imageViewCompass) ImageView image;
    @Bind(R.id.imageViewArrow) ImageView imageArrow;
    // TextView that will tell the user what degree is he heading
    @Bind(R.id.tvHeading) TextView tvHeading;
    //TextView that will tell the user the qibla degree
    @Bind(R.id.textViewQibla) TextView textQibla;
    // record the compass picture angle turned
    private float currentDegree = 0f;
    private float qiblaDegree = 0f;
            // device sensor manager
    private SensorManager mSensorManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // for the system's orientation sensor registered listeners
        qiblaDegree = Utility.getQiblaDirection(getApplicationContext());
        textQibla.setText(getString(R.string.qibla_direction)+ ": " + String.format(getString(R.string.format_qibla_dir),qiblaDegree));
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
        tvHeading.setText("Current Heading: " + String.format(getString(R.string.format_heading),degree));
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                        -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
        0.5f);
        // how long the animation will take place
        ra.setDuration(210);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        image.startAnimation(ra);
        RotateAnimation raArrow = new RotateAnimation(
                currentDegree + qiblaDegree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        raArrow.setDuration(210);
        raArrow.setFillAfter(true);
        imageArrow.startAnimation(raArrow);
        currentDegree = -degree;


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not used
    }
}
