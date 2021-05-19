package org.arg.LocationExample;

import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;

import org.arg.locationHandler.LocationHandler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationHandler
                .getInstance(this)
                .setListener(new LocationHandler.LocationListener() {
                    @Override
                    public void onUpdate(Location mLocation) {

                    }

                    @Override
                    public void onError(String mError) {

                    }

                    @Override
                    public void onGpsChange(boolean isGpsEnable) {

                    }
                }).startLocationRequest();
    }
}