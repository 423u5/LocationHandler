package org.arg.locationHandler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHandler {

    private static final String         TAG                         = LocationHandler.class.getSimpleName();
    public static final int             _PERMISSION_REQ             = 1001;
    public static final int             _SETTING_REQ                = 1002;
    private static final String         _REQUEST_PERMISSION         = "Permission not granted. Request permission";
    private static final String         _REQUEST_SETTING_PERMISSION = "Permission denied. Request from setting";
    private static final String[]       mPermissions                = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                                                   Manifest.permission.ACCESS_COARSE_LOCATION};

    @SuppressLint("StaticFieldLeak")
    private static LocationHandler mInstance;
    private final Context               mContext;

    private GoogleApiClient             mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private SettingsClient              mSettingClient;
    private LocationRequest             mLocationRequest;
    private LocationManager             mLocationManager;

    private LocationListener            mLocationListener;

    private int                         mInterval           = 1000;
    private int                         mLocationPriority   = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private boolean                     isFirstTimePermissionRequest;

    private LocationHandler(Context mContext){
        this.mContext = mContext;
    }

    public static LocationHandler getInstance(Context mContext) {
        if (mInstance == null)
            mInstance = new LocationHandler(mContext);
        return mInstance;
    }

    /**
     * Set the locationRequestInterval in Milliseconds
     * ByDefaultValue 1000ms
     * */
    public LocationHandler setLocationRequestInterval(int mInterval){
        this.mInterval = mInterval;
        return mInstance;
    }

    /**
     * Set the location priority by LocationRequest.PRIORITY
     * ByDefaultValue LocationRequest.PRIORITY_HIGH_ACCURACY
     * */
    public LocationHandler setLocationRequestPriority(int mLocationPriority) {
        this.mLocationPriority = mLocationPriority;
        return mInstance;
    }

    /**
     * Set the LocationListener for updating Location
     * */
    public LocationHandler setListener(LocationListener mLocationListener){
        this.mLocationListener = mLocationListener;
        return mInstance;
    }


    public void startLocationRequest() {
        if (hasLocationPermission())
            init();
        else if (canRequestPermission())
            requestPermission();
        else
            mLocationListener.onError(_REQUEST_SETTING_PERMISSION);
    }

    public void stopLocationRequest(){
        if(mGoogleApiClient != null)
            LocationServices.getFusedLocationProviderClient(mContext).removeLocationUpdates(mLocationCallback);
    }

    /**
     * Enable Gps Change Listener
     * Don't forget to disable when Activity close
     * */
    public LocationHandler enableGpsChangeCallback() {
        IntentFilter mIntentFilter = new IntentFilter(LocationManager.KEY_LOCATION_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        ((Activity) mContext).registerReceiver(mBroadcastReceiver, mIntentFilter);
        return mInstance;
    }

    /**
     * Disable Gps Change Listener
     * */
    public void disableGpsChangeCallback() {
        ((Activity) mContext).unregisterReceiver(mBroadcastReceiver);
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            for (String mPermission : mPermissions)
                if (mContext.checkSelfPermission(mPermission) == PackageManager.PERMISSION_DENIED)
                    return false;
        return true;
    }

    private boolean canRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            for (String mPermission : mPermissions)
                if (!((Activity) mContext).shouldShowRequestPermissionRationale(mPermission) &&
                        mContext.checkSelfPermission(mPermission) == PackageManager.PERMISSION_DENIED && isFirstTimePermissionRequest)
                    return false;
        return true;
    }

    private void requestPermission() {
        isFirstTimePermissionRequest = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            ((Activity) mContext).requestPermissions(mPermissions, _PERMISSION_REQ);
    }

    private boolean isGpsEnable(){
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private synchronized void init(){

        if (mLocationManager == null)
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        mSettingClient               = LocationServices.getSettingsClient(mContext);

        mGoogleApiClient             = new GoogleApiClient
                .Builder(mContext)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .addApi(LocationServices.API)
                .build();
    }

    private final GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @SuppressLint("RestrictedApi")
        @Override
        public void onConnected(@Nullable Bundle bundle) {

            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(mInterval);
            mLocationRequest.setPriority(mLocationPriority);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            builder.setAlwaysShow(true);
            LocationSettingsRequest mLocationSettingRequest = builder.build();

            mSettingClient.checkLocationSettings(mLocationSettingRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            requestLocationUpdate();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            switch (((ApiException) e).getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                        resolvableApiException.startResolutionForResult(((Activity) mContext), 123);
                                    } catch (IntentSender.SendIntentException sendIntentException) {
                                        mLocationListener.onError(sendIntentException.toString());
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    mLocationListener.onError("Location settings are inadequate, and cannot be fixed here. Fix in Settings.");
                                    break;
                                default:
                                    mLocationListener.onError(e.toString());
                            }
                        }
                    });
        }

        @Override
        public void onConnectionSuspended(int i) {
            connectGoogleClient();
        }
    };


    private final GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            mLocationListener.onError("Connection failed===\n" + connectionResult.toString());
        }
    };

    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            mLocationListener.onUpdate(locationResult.getLastLocation());
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PROVIDER_CHANGED))
                mLocationListener.onGpsChange(isGpsEnable());
        }
    };

    private void connectGoogleClient(){
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        if (googleAPI.isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS)
            mGoogleApiClient.connect();
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdate(){
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
    }

    public interface LocationListener{
        void onUpdate(android.location.Location mLocation);
        void onError(String mError);
        void onGpsChange(boolean isGpsEnable);
    }

}
