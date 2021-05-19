package org.arg.LocationExample;

import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

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
                        toast("Loc " + mLocation.toString());
                    }

                    @Override
                    public void onError(String mError) {
                        toast("Error " + mError);
                    }

                    @Override
                    public void onGpsChange(boolean isGpsEnable) {
                        toast( "isGpsEnable " + isGpsEnable);
                    }
                }).startLocationRequest();

    }

    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}