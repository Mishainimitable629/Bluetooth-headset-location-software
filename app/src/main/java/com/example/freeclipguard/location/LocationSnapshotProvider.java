package com.example.freeclipguard.location;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.CancellationSignal;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.Nullable;

import com.example.freeclipguard.model.LocationSnapshot;
import com.example.freeclipguard.util.PermissionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LocationSnapshotProvider {

    private static final long DEFAULT_CURRENT_LOCATION_TIMEOUT_MS = 4_000L;
    private static final long FRESH_LOCATION_AGE_MS = 20_000L;
    private static final float DESIRABLE_ACCURACY_METERS = 60F;

    private LocationSnapshotProvider() {
    }

    @Nullable
    public static LocationSnapshot getFreshSnapshot(Context context) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return null;
        }
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            return null;
        }
        List<String> providers = locationManager.getProviders(true);
        if (providers == null || providers.isEmpty()) {
            return null;
        }

        Location currentBest = getCurrentBestLocation(locationManager, providers, DEFAULT_CURRENT_LOCATION_TIMEOUT_MS);
        Location lastKnownBest = getBestLastKnownLocation(locationManager, providers);
        Location chosenLocation = choosePreferredLocation(currentBest, lastKnownBest);
        return toSnapshot(chosenLocation);
    }

    @Nullable
    @SuppressLint("MissingPermission")
    public static LocationSnapshot getBestEffortSnapshot(Context context) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            return null;
        }
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (locationManager == null) {
            return null;
        }
        List<String> providers = locationManager.getProviders(true);
        return toSnapshot(getBestLastKnownLocation(locationManager, providers));
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getCurrentBestLocation(LocationManager locationManager,
            List<String> providers,
            long timeoutMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }
        List<Location> currentLocations = Collections.synchronizedList(new ArrayList<>());
        List<CancellationSignal> cancellationSignals = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(providers.size());
        for (String provider : providers) {
            CancellationSignal cancellationSignal = new CancellationSignal();
            cancellationSignals.add(cancellationSignal);
            try {
                locationManager.getCurrentLocation(provider, cancellationSignal, Runnable::run, location -> {
                    if (location != null) {
                        currentLocations.add(location);
                    }
                    countDownLatch.countDown();
                });
            }
            catch (Exception ignored) {
                countDownLatch.countDown();
            }
        }
        try {
            countDownLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }
        catch (Exception ignored) {
        }
        finally {
            for (CancellationSignal cancellationSignal : cancellationSignals) {
                cancellationSignal.cancel();
            }
        }
        return selectBestLocation(currentLocations);
    }

    @Nullable
    @SuppressLint("MissingPermission")
    private static Location getBestLastKnownLocation(LocationManager locationManager, List<String> providers) {
        List<Location> locations = new ArrayList<>();
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null) {
                    locations.add(candidate);
                }
            }
            catch (Exception ignored) {
            }
        }
        return selectBestLocation(locations);
    }

    @Nullable
    private static Location choosePreferredLocation(@Nullable Location currentLocation, @Nullable Location lastKnownLocation) {
        if (currentLocation == null) {
            return lastKnownLocation;
        }
        if (lastKnownLocation == null) {
            return currentLocation;
        }

        boolean currentIsFreshAndAccurate = isFresh(currentLocation) && currentLocation.getAccuracy() <= DESIRABLE_ACCURACY_METERS;
        if (currentIsFreshAndAccurate) {
            return currentLocation;
        }

        return scoreLocation(currentLocation) >= scoreLocation(lastKnownLocation)
                ? currentLocation
                : lastKnownLocation;
    }

    @Nullable
    private static Location selectBestLocation(List<Location> locations) {
        Location bestLocation = null;
        for (Location candidate : locations) {
            if (candidate == null) {
                continue;
            }
            if (bestLocation == null || scoreLocation(candidate) > scoreLocation(bestLocation)) {
                bestLocation = candidate;
            }
        }
        return bestLocation;
    }

    private static int scoreLocation(Location location) {
        long ageMs = Math.max(0L, System.currentTimeMillis() - location.getTime());
        int score = 0;

        score -= Math.round(Math.min(location.getAccuracy(), 500F) * 2F);
        score -= (int) Math.min(ageMs / 1000L, 600L);

        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            score += 35;
        }
        else if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            score += 10;
        }

        if (ageMs <= FRESH_LOCATION_AGE_MS) {
            score += 25;
        }
        if (location.getAccuracy() <= 20F) {
            score += 30;
        }
        else if (location.getAccuracy() <= DESIRABLE_ACCURACY_METERS) {
            score += 15;
        }

        return score;
    }

    private static boolean isFresh(Location location) {
        return System.currentTimeMillis() - location.getTime() <= FRESH_LOCATION_AGE_MS;
    }

    @Nullable
    private static LocationSnapshot toSnapshot(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return new LocationSnapshot(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getTime(),
                location.getProvider()
        );
    }
}
