package com.codephillip.mapboxhelloapp;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MapBoxApp";
    private boolean isEndNotified;
    private ProgressBar progressBar;
    private MapView mapView;
    private OfflineManager offlineManager;

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {

                mapboxMap.setStyle(Style.OUTDOORS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

// Set up the OfflineManager
                        offlineManager = OfflineManager.getInstance(MainActivity.this);

// Create a bounding box for the offline region
                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                .include(new LatLng(0.0707, 32.3697)) // Northeast
                                .include(new LatLng(0.4851, 32.9058)) // Southwest
                                .build();

// Define the offline region
                        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                                style.getUrl(),
                                latLngBounds,
                                10,
                                20,
                                MainActivity.this.getResources().getDisplayMetrics().density);

// Set the metadata
                        byte[] metadata;
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(JSON_FIELD_REGION_NAME, "Yosemite National Park");
                            String json = jsonObject.toString();
                            metadata = json.getBytes(JSON_CHARSET);
                        } catch (Exception exception) {
                            Log.e("Failed to encode ", exception.getMessage());
                            metadata = null;
                        }

// Create the region asynchronously
                        offlineManager.createOfflineRegion(
                                definition,
                                metadata,
                                new OfflineManager.CreateOfflineRegionCallback() {
                                    @Override
                                    public void onCreate(OfflineRegion offlineRegion) {
                                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

// Display the download progress bar
                                        progressBar = findViewById(R.id.progress_bar);
                                        startProgress();

// Monitor the download progress using setObserver
                                        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                                            @Override
                                            public void onStatusChanged(OfflineRegionStatus status) {

// Calculate the download percentage and update the progress bar
                                                double percentage = status.getRequiredResourceCount() >= 0
                                                        ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                                                        0.0;

                                                if (status.isComplete()) {
// Download complete
                                                    endProgress(getString(R.string.simple_offline_end_progress_success));
                                                } else if (status.isRequiredResourceCountPrecise()) {
// Switch to determinate state
                                                    setPercentage((int) Math.round(percentage));
                                                }
                                            }

                                            @Override
                                            public void onError(OfflineRegionError error) {
// If an error occurs, print to logcat
                                                Log.e("onError reason: ", error.getReason());
                                                Log.e("onError message: ", error.getMessage());
                                            }

                                            @Override
                                            public void mapboxTileCountLimitExceeded(long limit) {
// Notify if offline region exceeds maximum tile count
                                                Log.e("Mapbox tile count", String.valueOf(limit));
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e("Error: %s", error);
                                    }
                                });
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (offlineManager != null) {
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                @Override
                public void onList(OfflineRegion[] offlineRegions) {
                    if (offlineRegions.length > 0) {
// delete the last item in the offlineRegions list which will be yosemite offline map
                        offlineRegions[(offlineRegions.length - 1)].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                            @Override
                            public void onDelete() {
                                Toast.makeText(
                                        MainActivity.this,
                                        getString(R.string.basic_offline_deleted_toast),
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("On delete error: %s", error);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("onListError: %s", error);
                }
            });
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Progress bar methods
    private void startProgress() {

// Start and show the progress bar
        isEndNotified = false;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setPercentage(final int percentage) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);
    }

    private void endProgress(final String message) {
// Don't notify more than once
        if (isEndNotified) {
            return;
        }

// Stop and hide the progress bar
        isEndNotified = true;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

// Show a toast
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}