package com.bankingapp.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * LocationHelper — wraps LocationManager to provide the last-known
 * latitude and longitude for attaching to transactions.
 *
 * Syllabus: LocationManager, GPS_PROVIDER, NETWORK_PROVIDER,
 *           requestLocationUpdates(), getLastKnownLocation()
 *
 * Usage:
 *   LocationHelper locHelper = new LocationHelper(context);
 *   locHelper.startUpdates();           // start listening
 *   double lat = locHelper.getLatitude();
 *   double lng = locHelper.getLongitude();
 *   locHelper.stopUpdates();            // release when done
 */
public class LocationHelper implements LocationListener {

    private static final String TAG = "LocationHelper";

    // Minimum update interval and distance to request from provider
    private static final long   MIN_TIME_MS   = 5000;   // 5 seconds
    private static final float  MIN_DIST_M    = 5.0f;   // 5 metres

    private final Context         context;
    private final LocationManager locationManager;

    private double latitude  = 0.0;
    private double longitude = 0.0;
    private boolean updatesStarted = false;

    public LocationHelper(Context context) {
        this.context         = context.getApplicationContext();
        this.locationManager = (LocationManager)
                this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    // ── Start listening for location updates ───────────────────────────
    /**
     * Call this in onResume() or when you need fresh location data.
     * Tries GPS first, falls back to NETWORK provider.
     * Also reads the last-known location immediately so we have
     * something even before the first update fires.
     */
    public void startUpdates() {
        if (!hasPermission()) {
            Log.w(TAG, "Location permission not granted — skipping startUpdates()");
            return;
        }
        if (updatesStarted) return;

        try {
            // GPS provider — most accurate; may be slow indoors
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DIST_M,
                        this);
                Location last = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER);
                if (last != null) updateCoordinates(last);
            }

            // Network provider — faster fix; less accurate
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DIST_M,
                        this);
                Location last = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER);
                if (last != null) updateCoordinates(last);
            }

            updatesStarted = true;
            Log.d(TAG, "Location updates started");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException starting location updates", e);
        }
    }

    // ── Stop listening (call in onPause() / onDestroy()) ──────────────
    public void stopUpdates() {
        if (updatesStarted) {
            locationManager.removeUpdates(this);
            updatesStarted = false;
            Log.d(TAG, "Location updates stopped");
        }
    }

    // ── LocationListener callbacks ─────────────────────────────────────
    @Override
    public void onLocationChanged(Location location) {
        updateCoordinates(location);
        Log.d(TAG, "Location updated: " + latitude + ", " + longitude);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, provider + " enabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, provider + " disabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Required for API < 29; no-op for modern Android
    }

    // ── Getters ───────────────────────────────────────────────────────
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }

    /** Returns true if we have a real fix (non-zero coordinates). */
    public boolean hasValidLocation() {
        return latitude != 0.0 || longitude != 0.0;
    }

    /** Formatted string for display: "12.971599, 77.594566" */
    public String getFormattedLocation() {
        if (!hasValidLocation()) return "Location unavailable";
        return String.format(java.util.Locale.getDefault(),
                "%.6f, %.6f", latitude, longitude);
    }

    // ── Private helpers ───────────────────────────────────────────────
    private void updateCoordinates(Location location) {
        latitude  = location.getLatitude();
        longitude = location.getLongitude();
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
