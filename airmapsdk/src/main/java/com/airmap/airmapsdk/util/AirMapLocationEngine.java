package com.airmap.airmapsdk.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;

import timber.log.Timber;

public class AirMapLocationEngine extends LocationEngine {

    private static AirMapLocationEngine instance;

    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;

    private AirMapLocationEngine(Context context) {
        super();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                Timber.d("onLocationAvailability: " + locationAvailability);
            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Timber.d("onLocationResult: " + locationResult);

                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLocations().get(0);
                    for (LocationEngineListener listener : locationListeners) {
                        listener.onLocationChanged(location);
                    }

                    removeLocationUpdates();
                }
            }
        };

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public static synchronized AirMapLocationEngine getLocationEngine(Context context) {
        if (instance == null) {
            instance = new AirMapLocationEngine(context.getApplicationContext());
        }
        return instance;
    }

    public void setLocationRequest(LocationRequest locationRequest) {
        this.locationRequest = locationRequest;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void activate() {
        Timber.d("activate");
        requestLocationUpdates();
    }

    @Override
    public void deactivate() {
        Timber.d("deactivate");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @SuppressLint("MissingPermission")
    public void getLastKnownLocation() {
        Task<Location> locationTask = fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    Timber.d("getLastLocationTask success: " + location);
                    if (location != null) {
                        for (LocationEngineListener listener : locationListeners) {
                            listener.onLocationChanged(location);
                        }
                    }
                });

        if (locationTask.isComplete()) {
            if (locationTask.getResult() != null) {
                Timber.d("already has result: " + locationTask.getResult());
                for (LocationEngineListener listener : locationListeners) {
                    listener.onLocationChanged(locationTask.getResult());
                }
            } else if (locationTask.getException() != null) {
                Timber.e(locationTask.getException());
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastLocation() {
        Task<Location> task = fusedLocationClient.getLastLocation();
        if (task.isComplete()) {
            return task.getResult();
        }

        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void requestLocationUpdates() {
        Timber.d("locationRequestLocationUpdates w/ priority: " + locationRequest.getPriority());
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void removeLocationUpdates() {
        Timber.d("removeLocationUpdates");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public Type obtainType() {
        return Type.GOOGLE_PLAY_SERVICES;
    }
}
