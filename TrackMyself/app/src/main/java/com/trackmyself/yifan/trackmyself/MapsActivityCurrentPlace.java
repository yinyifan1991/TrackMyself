package com.trackmyself.yifan.trackmyself;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        StopTrackDialogFragment.StopTrackDialogListener {

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";

    // Used for selecting the current place.
    private final int mMaxEntries = 5;
    private String[] mLikelyPlaceNames = new String[mMaxEntries];
    private String[] mLikelyPlaceAddresses = new String[mMaxEntries];
    private String[] mLikelyPlaceAttributions = new String[mMaxEntries];
    private LatLng[] mLikelyPlaceLatLngs = new LatLng[mMaxEntries];

    //Get location
    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;
    LocationSettingsRequest mLocationSettingsRequest;
    SettingsClient mSettingsClient;
    LocationCallback mLocationCallBack;
    Location mCurrentLocation;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private boolean mRequestingLocationUpdates;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    private final static String KEY_LAST_MARKER = "last-marker-location";
    private final static String KEY_FIRST_LOCATION_UPDATE = "first-location-update";
    private final static String KEY_START_TRACK_TIME = "start-track-time";
    private final static String KEY_END_TRACK_TIME = "end-track-time";


    private SharedPreferences mPrefs;

    //Objects for labeling
    private Location lastLocationShow;
    private static final int LEAST_DISTANCE_TO_MARK = 2;

    //Objects for routes
    //private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;

    //Counters to tell state
    private boolean firstLocationUpdate;

    //Tracking task control
    private Button buttonStart;
    private Button buttonStop;
    private boolean trackStart;

    //Tracking result
    private String startTrackTime = "";
    private String endTrackTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("onCreate", "123");
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        /*
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        */
        //Label the first time updating location
        firstLocationUpdate = true;
        //Label the start of tracking task
        trackStart = false;

        // Retrieve the content view that renders the map.
        setContentView(R.layout.current_place);

        buttonStart = (Button)findViewById(R.id.button_start);
        buttonStop = (Button)findViewById(R.id.button_stop);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();

        mRequestingLocationUpdates = true;
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationRequest();
        buildLocationSettingsRequest();
        createLocationCallBack();
        mPrefs = getPreferences(Activity.MODE_PRIVATE);

        mPolylineOptions = new PolylineOptions().width(5).color(Color.RED).geodesic(true);

    }


    private void updateValuesFromBundle (Bundle savedInstanceState) {
        lastLocationShow = null;
        Log.i("retrieve state", "== null");
        if(savedInstanceState != null) {
            Log.i("retrieve state", "!= null");
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                Log.i("retrieve state", "contains");
                mRequestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES);
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
                lastLocationShow = mLastKnownLocation;
            }

            updateLocationUI();
        }
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            Log.i("save state", "success");
            outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            showCurrentPlace();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        if(map == null) return;
        Log.i("onMapReady", "123");
        mMap = map;

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        addListenerOnButtons();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        /*
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Log.v("Latitude", Double.toString(location.getLatitude()));
                            mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("Home"));
                        }
                    }
                });
                */
        /*
        if(mLastKnownLocation != null && lastLocationShow != null && checkDistance(mLastKnownLocation, lastLocationShow)) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                    .title("Home"));
        }
        */
    }


    @Override
    public void onResume() {
        super.onResume();
        //SharedPreferences mPrefs = getPreferences(Activity.MODE_PRIVATE);
        //SharedPreferences mPrefs = getSharedPreferences("first pref", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        String camLoc = mPrefs.getString(KEY_CAMERA_POSITION, "");
        String loc = mPrefs.getString(KEY_LOCATION, "");
        String lastMarkerLoc = mPrefs.getString(KEY_LAST_MARKER, "");
        //String polylineOptions = mPrefs.getString(KEY_POLYLINE_OPTIONS, "");
        //if(camLoc.equals("")) Log.i("camera position", "null");
        if(!camLoc.equals("")) mCameraPosition = gson.fromJson(camLoc, CameraPosition.class);
        if(!loc.equals("")) mLastKnownLocation = gson.fromJson(loc, Location.class);
        if(!lastMarkerLoc.equals("")) lastLocationShow = gson.fromJson(lastMarkerLoc, Location.class);
        //if(!polylineOptions.equals("")) mPolylineOptions = gson.fromJson(polylineOptions, PolylineOptions.class);
        mRequestingLocationUpdates = mPrefs.getBoolean(KEY_REQUESTING_LOCATION_UPDATES, true);
        firstLocationUpdate = mPrefs.getBoolean(KEY_FIRST_LOCATION_UPDATE, true);
        Log.i("onResume", Boolean.toString(mRequestingLocationUpdates));
        if(mRequestingLocationUpdates && checkPermission()) {
            startLocationUpdate();
        } else if(!checkPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onStop() {

        super.onStop();
        Log.i("onStop", "123");
    }
    @Override
    public void onPause() {

        super.onPause();
        Log.i("onPause", "123");
        //SharedPreferences mPrefs = getPreferences(Activity.MODE_PRIVATE);
        //SharedPreferences mPrefs = getSharedPreferences("first pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor ed = mPrefs.edit();
        Gson gson = new Gson();
        String camLoc = gson.toJson(mMap.getCameraPosition());
        String loc = gson.toJson(mLastKnownLocation);
        String lastMakerLoc = gson.toJson(lastLocationShow);
        //String polylineOptions = gson.toJson(mPolylineOptions);
        //if(json.equals("")) Log.i("save camera", "null");
        ed.putString(KEY_CAMERA_POSITION, camLoc);
        ed.putString(KEY_LOCATION, loc);
        ed.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        ed.putString(KEY_LAST_MARKER, lastMakerLoc);
        //ed.putString(KEY_POLYLINE_OPTIONS, polylineOptions);
        ed.putBoolean(KEY_FIRST_LOCATION_UPDATE, firstLocationUpdate);
        ed.apply();
        //stopLocationUpdates();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if(dialog == null) return;
        trackStart = false;
        endTrackTime = DateFormat.getDateTimeInstance().format(new Date());
        if(mLastKnownLocation != null) addMarker("end");
        dialog.dismiss();
        switchToResult();
    }

    @Override
    public void onDialopNegativeClick(DialogFragment dialog) {
        if(dialog == null) return;
        dialog.dismiss();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        Log.i("getDeviceLocation", "");
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.i("requestPermission", "123");
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission")
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    int i = 0;
                    mLikelyPlaceNames = new String[mMaxEntries];
                    mLikelyPlaceAddresses = new String[mMaxEntries];
                    mLikelyPlaceAttributions = new String[mMaxEntries];
                    mLikelyPlaceLatLngs = new LatLng[mMaxEntries];
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        // Build a list of likely places to show the user. Max 5.
                        mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                        mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace().getAddress();
                        mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
                                .getAttributions();
                        mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                        i++;
                        if (i > (mMaxEntries - 1)) {
                            break;
                        }
                    }
                    // Release the place likelihood buffer, to avoid memory leaks.
                    likelyPlaces.release();

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog();
                }
            });
        } else {
            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The "which" argument contains the position of the selected item.
                        LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                        String markerSnippet = mLikelyPlaceAddresses[which];
                        if (mLikelyPlaceAttributions[which] != null) {
                            markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                        }
                        // Add a marker for the selected place, with an info window
                        // showing information about that place.
                        mMap.addMarker(new MarkerOptions()
                                .title(mLikelyPlaceNames[which])
                                .position(markerLatLng)
                                .snippet(markerSnippet));

                        // Position the map's camera at the location of the marker.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                                DEFAULT_ZOOM));
                    }
                };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        Log.i("updateLocationUI", "123");
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
        if(lastLocationShow == null) lastLocationShow = mLastKnownLocation;
        if(trackStart) {
            if(mLastKnownLocation != null && lastLocationShow != null && checkDistance(lastLocationShow, mLastKnownLocation)) {
                addMarker("halfway");
                if(!firstLocationUpdate) drawPath(lastLocationShow, mLastKnownLocation);
                else firstLocationUpdate = false;

            }
        }
        lastLocationShow = mLastKnownLocation;

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    protected void createLocationCallBack() {
        Log.i("locationCallBack", "123");
        mLocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for(Location location: locationResult.getLocations()) {
                    mLastKnownLocation = location;
                    Log.i("locatinCallBack", "loop");
                    updateLocationUI();
                }
            }
        };
    }

    private void startLocationUpdate() {
        Log.i("startLocationUpdate", "123");
        Task<LocationSettingsResponse> task = mSettingsClient.checkLocationSettings(mLocationSettingsRequest);

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                Log.i(TAG, "All location settings are satisfied.");
                // location requests here.
                // ...
                if(checkPermission()) mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                updateLocationUI();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapsActivityCurrentPlace.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        mRequestingLocationUpdates = false;

                    updateLocationUI();
                }
            }
        });
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallBack);
    }

    private boolean checkPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkDistance(Location last, Location current) {
        double lat1 = last.getLatitude();
        double lat2 = current.getLatitude();
        double lon1 = last.getLongitude();
        double lon2 = current.getLongitude();
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515 * 1000;
        Log.i("distance", Double.toString(dist));
        if(dist > LEAST_DISTANCE_TO_MARK) return true;
        return false;
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    private void addMarker(String tag) {
        if(mLastKnownLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                    .title(tag));
        }
    }

    private void drawPath(Location lastLoc, Location currentLoc) {
        if(mMap == null) return;
        if(mPolylineOptions == null) {
            Log.i("drawPath", "option is null");
            mPolylineOptions = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
        }
        Log.i("drawPath", "not null");
        mPolylineOptions.add(new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude()));
        mPolylineOptions.add(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()));
        mMap.addPolyline(mPolylineOptions);
    }

    private void addListenerOnButtons() {
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("onClick", "buttonStart");
                trackStart = true;
                startTrackTime = DateFormat.getDateTimeInstance().format(new Date());
                if(mLastKnownLocation != null) addMarker("start");
            }
        });
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("onClick", "buttonStop");
                showStopTrackDialog("Track is stopping", "Is this the end of this trip?");
            }
        });
    }

    private void showStopTrackDialog(String title, String message) {
        FragmentManager fm = getSupportFragmentManager();
        StopTrackDialogFragment dialog = StopTrackDialogFragment.newInstance(title, message);
        dialog.show(fm, "fragment_alert");
    }

    private void switchToResult() {
        Intent intent = new Intent(this, TrackResult.class);
        intent.putExtra(KEY_START_TRACK_TIME, startTrackTime);
        intent.putExtra(KEY_END_TRACK_TIME, endTrackTime);
        startActivity(intent);
    }
}
