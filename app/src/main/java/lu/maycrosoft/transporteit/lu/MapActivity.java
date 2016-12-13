package lu.maycrosoft.transporteit.lu;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private GoogleApiClient client;

    private final String TAG = "MapActivity";

    private String VELOHSTATIONS = "VELOH";
    private String BUSSTATIONS = "BUS";

    private ArrayList<BusStation> allBusStations;
    private ArrayList<VelohStation> allVelohStations;

    private ArrayList<Marker> allBusMarkers;
    private ArrayList<Marker> allVelohMarkers;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        allBusMarkers = new ArrayList<>();
        allVelohMarkers = new ArrayList<>();


        JsonURLHandler jsonBusURLHandler = new JsonURLHandler(BUSSTATIONS);
        JsonURLHandler jsonVelohURLHandler = new JsonURLHandler(VELOHSTATIONS);
        try {

            String busJSON = jsonBusURLHandler.execute().get();
            String velohJSON = jsonVelohURLHandler.execute().get();

            allBusStations = getBusStationsFromAsyncTaskResult(busJSON);
            allVelohStations = getVelohStationsFromAsyncTaskResult(velohJSON);

            Log.i("json", ""+(velohJSON.getBytes().length)/1024);
            Location loc = new Location("");
            loc.setLatitude(49.611622);
            loc.setLongitude(6.131935);

            for (int i=0; i<allVelohStations.size(); i++){


                Log.i("json", allVelohStations.get(i).getLatLng().toString());
                Log.i("json", allVelohStations.get(i).getStationName());
                //Log.i("json", ""+allBusStations.get(i).getDistanceInMetersFromCurrentLocationToBusStation(loc));
                //Log.i("json", ""+loc);
                Log.i("json", "------------------------------------------");

            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }



    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://lu.maycrosoft.transporteit.lu/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://lu.maycrosoft.transporteit.lu/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();

        // Add a marker in Sydney and move the camera
        LatLng coordinateLuxembourg = new LatLng(49.611622,6.131935);
        addBusMarkerFromLocation(coordinateLuxembourg, "Luxembourg", "Some random description");


        LatLng coordinateLuxembourgVeloh = new LatLng(49.611622,6.134950);
        addVelohMarkerFromLocation(coordinateLuxembourgVeloh, "Luxembourg", "Some random description");

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

                VisibleRegion vr = mMap.getProjection().getVisibleRegion();

                float distance = (float) distanceFromTwoLatLngInKM(vr.latLngBounds.northeast, vr.latLngBounds.southwest);
                Log.d(TAG, "distanceInMeters " + distance);
                float alphaValue = (1 - distance / 100f);

                Log.d(TAG, "alphaValue " + alphaValue);

                for (int i = 0; i < allBusMarkers.size(); i++) {
                    allBusMarkers.get(i).setAlpha(alphaValue);
                }
                for (int i = 0; i < allVelohMarkers.size(); i++) {
                    allVelohMarkers.get(i).setAlpha(alphaValue);
                }
            }
        });

    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.

            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    private void zoomBetweenTwoLatLngs(LatLng latLng1, LatLng latLng2){

        //Boundaries from both LatLngs
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(latLng1);
        builder.include(latLng2);
        LatLngBounds bounds = builder.build();

        //Animate/Move camera to boundaries - 100 pixels (so 50 more to space at each side)
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }

        sharedPreferences = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    private void addBusMarkerFromLocation(LatLng coordinates, String title, String description) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(coordinates).alpha(0.7f));
        marker = setMarkerTitleAndSnippet(marker, title, description);
        allBusMarkers.add(marker);
    }


    private void addVelohMarkerFromLocation(LatLng coordinates, String title, String description) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(coordinates).alpha(0.7f));
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        marker = setMarkerTitleAndSnippet(marker, title, description);
        allVelohMarkers.add(marker);
    }

    private Marker setMarkerTitleAndSnippet(Marker marker, String title, String description){
        marker.setTitle(title);
        marker.setSnippet(description);
        return marker;
    }


    private ArrayList<BusStation> getBusStationsFromAsyncTaskResult(String json){
        ArrayList<BusStation> busStations = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray listOfStations = (JSONArray) obj.get("stations");
            for(int i=0; i<listOfStations.length(); i++){

                JSONObject station = listOfStations.getJSONObject(i);

                BusStation busStation = new BusStation((String)station.get("id"),
                        (String)station.get("stationName"),
                        Double.valueOf((String)station.get("lat")),
                        Double.valueOf((String)station.get("long")));
                busStations.add(busStation);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return busStations;
    }

    private ArrayList<VelohStation> getVelohStationsFromAsyncTaskResult(String json){
        ArrayList<VelohStation> velohStations = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray listOfStations = (JSONArray) obj.get("stations");
            for(int i=0; i<listOfStations.length(); i++){

                JSONObject station = listOfStations.getJSONObject(i);

                VelohStation velohStation = new VelohStation((Integer)station.get("number"),
                        (String)station.get("name"),
                        (String)station.get("address"),
                        (Double) station.get("latitude"),
                        (Double) station.get("longitude"));
                velohStations.add(velohStation);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return velohStations;
    }

    private double distanceFromTwoLatLngInKM(LatLng latLng1, LatLng latLng2){
        double lat1=latLng1.latitude;
        double lat2=latLng2.latitude;
        double lon1=latLng1.longitude;
        double lon2=latLng2.longitude;

        int R = 6373; // radius of the earth in kilometres
        double lat1rad = Math.toRadians(lat1);
        double lat2rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2-lat1);
        double deltaLon = Math.toRadians(lon2-lon1);

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1rad) * Math.cos(lat2rad) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }


    private void storeIntValueToSharedPreferences(String key, int value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void storeBooleanValueToSharedPreferences(String key, boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void storeFloatValueToSharedPreferences(String key, float value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    private int getIntValueFromSharedPreferences(String key, int defaultValue){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defaultValue);
    }

    private boolean getBooleanValueFromSharedPreferences(String key, boolean defaultValue){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, defaultValue);

    }

    private float getFloatValueFromSharedPreferences(String key, float defaultValue){
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getFloat(key, defaultValue);
    }


}
