package lu.maycrosoft.transporteit.lu;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback, FinishedDownloadListener ,CompoundButton.OnCheckedChangeListener{

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

    private final String showBusStationPreference = "showBusStationPreference";
    private final String showVelohStationPreference = "showVelohStationPreference";

    Menu navigationMenu;

    SwitchCompat busSwitch;
    SwitchCompat velohSwitch;

    private float oldAlphaValue=0;
    private ArrayList<String> stationNames;

    ArrayList<Marker> favourites;


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

        favourites = new ArrayList<>();


        sharedPreferences = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        loadBusAndVelohsFromURL();

        NavigationView navigationView=(NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationMenu = navigationView.getMenu();
        createAndLoadSwitches();
        addClickListenerToMenuItems();


    }

    private void loadBusAndVelohsFromURL() {
        JsonURLHandler jsonBusURLHandler = new JsonURLHandler(BUSSTATIONS);
        JsonURLHandler jsonVelohURLHandler = new JsonURLHandler(VELOHSTATIONS);

        jsonBusURLHandler.addListener(this);
        jsonVelohURLHandler.addListener(this);
        try {

            String busJSON = jsonBusURLHandler.execute().get();
            String velohJSON = jsonVelohURLHandler.execute().get();
            allBusStations = getBusStationsFromAsyncTaskResult(busJSON);
            allVelohStations = getVelohStationsFromAsyncTaskResult(velohJSON);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            public void run() {
                sortBusStationsByName();
            }
        }).start();

    }

    private void sortBusStationsByName() {
        Collections.sort(allBusStations, new Comparator() {
            @Override
            public int compare(Object softDrinkOne, Object softDrinkTwo) {
                //use instanceof to verify the references are indeed of the type in question
                return ((BusStation)softDrinkOne).getStationName()
                        .compareTo(((BusStation)softDrinkTwo).getStationName());
            }
        });

    }

    private void createAndLoadSwitches() {
        View busSwitchView = getViewFromMenuItem(R.id.busSwitchItem);
        View velohSwitchView = getViewFromMenuItem(R.id.velohSwitchItem);

        busSwitch = (SwitchCompat) busSwitchView.findViewById(R.id.busSwitch);
        velohSwitch = (SwitchCompat) velohSwitchView.findViewById(R.id.velohSwitch);

        busSwitch.setOnCheckedChangeListener(this);
        velohSwitch.setOnCheckedChangeListener(this);

        boolean showBusStations = getBooleanValueFromSharedPreferences(showBusStationPreference,true);
        boolean showVelohStations = getBooleanValueFromSharedPreferences(showVelohStationPreference,true);

        busSwitch.setChecked(showBusStations);
        velohSwitch.setChecked(showVelohStations);

        showMarkersOnScreen(allBusMarkers,showBusStations);
        showMarkersOnScreen(allVelohMarkers,showVelohStations);
    }


    private void addClickListenerToMenuItems(){
    }

    private View getViewFromMenuItem(int id){
        MenuItem item = navigationMenu.findItem(id);
        return MenuItemCompat.getActionView(item);
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

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

                VisibleRegion vr = mMap.getProjection().getVisibleRegion();

                float distance = (float) distanceFromTwoLatLngInKM(vr.latLngBounds.northeast, vr.latLngBounds.southwest);
                Log.d(TAG, "distanceInMeters " + distance);

                /*TODO proportional alphavalue and going to 0 when 10 km and 1 when 0.5 km
                    check for the bachelor these and or trier height.
                   */
                //float alphaValue = Math.max(0,Math.min(1,(1 - distance / 50f)));

                float alphaValue = 1 - map(distance, 0.5f, 50.0f, 0.0f, 1.0f);


                Log.d(TAG, "alphaValue " + alphaValue);
                if(oldAlphaValue!=alphaValue) {
                    if (Math.abs(oldAlphaValue  - alphaValue ) > 0.1 || alphaValue == 1 || alphaValue == 0) {
                        oldAlphaValue = alphaValue;
                        Log.d(TAG, "alphaValue " + alphaValue);

                        for (int i = 0; i < allBusMarkers.size(); i++) {
                            allBusMarkers.get(i).setAlpha(alphaValue);

                        }
                        for (int i = 0; i < allVelohMarkers.size(); i++) {
                            allVelohMarkers.get(i).setAlpha(alphaValue);
                        }
                    }
                }
            }
        });

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                View v = getLayoutInflater().inflate(R.layout.marker_snippet, null);

                TextView title = (TextView) v.findViewById(R.id.title);
                title.setTextColor(Color.BLACK);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView info = (TextView) v.findViewById(R.id.info);

                if (allBusMarkers.contains(marker)){
                    String s = getBusInformationFromMarker(marker);
                    int counter = 0;
                    for( int i=0; i<s.length(); i++ ) {
                        if( s.charAt(i) == '\n' ) {
                            counter++;
                        }
                    }
                    if (counter > 32){
                        info.setTextSize(6f);
                    }

                    info.setText(getBusInformationFromMarker(marker));
                }
                else{
                    if (allVelohMarkers.contains(marker)){
                        info.setText(getVelohInformationFromMarker(marker));
                    }
                }


                return v;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {

            @Override
            public boolean onMarkerClick(Marker marker) {
                VisibleRegion vr = mMap.getProjection().getVisibleRegion();
                float distance = (float) distanceFromTwoLatLngInKM(vr.latLngBounds.northeast, vr.latLngBounds.southwest);

                // marker only clickable when close as 10km
                if(marker != null && distance < 10){
                    marker.showInfoWindow();
                    int zoom = (int)mMap.getCameraPosition().zoom;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude), zoom), 2000, null);
                }

                return true;
            }

        });


        mMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {

            @Override
            public void onInfoWindowLongClick(Marker marker) {

                if (!favourites.contains(marker)){
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    favourites.add(marker);

                }
                else{
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    favourites.remove(marker);
                }

            }
        });


    }

    private float map(float num, float input_min, float input_max, float output_min, float output_max) {
        if (num < input_min){
            num = input_min;
        }else{
            if (num > input_max){
                num = input_max;
            }
        }
        return ((num - input_min) * (output_max - output_min) / (input_max - input_min)) + output_min;
    }

    private String getVelohInformationFromMarker(Marker marker){
        String velohinfo = "";
        for (int i=0; i<allVelohStations.size(); i++){
            if (allVelohStations.get(i).getStationName().equals(marker.getTitle())){
                String velohName = allVelohStations.get(i).getStationName();
                String address = "Address: " + allVelohStations.get(i).getAddress();
                velohinfo = address + "\n";
                break;
            }
        }

        return velohinfo;
    }

    private String getBusInformationFromMarker(Marker marker){
        ArrayList<BusInformation> busInformations = new ArrayList<>();
        String busInfos = "";
        for(int i=0; i< allBusMarkers.size(); i++){
            if (allBusStations.get(i).getStationName().equals(marker.getTitle())){
                Log.i("MARKER CLICKED", marker.getTitle());

                busInformations = allBusStations.get(i).getBusInformations();

                for (int j=0; j<busInformations.size(); j++){
                    String busline = busInformations.get(j).getBusLine();
                    String times = "";
                    for (int k=0; k<busInformations.get(j).getTimes().size(); k++){
                        if(k < 4) {
                            times += busInformations.get(j).getTimes().get(k) + " ";
                        }
                        else break;
                    }

                    String stationName = busInformations.get(j).getDestinationStationName();

                    String busInformation = "Busline: " + busline + "\n" + "Departure Times: " + times + "\n" + "Destination: " + stationName + "\n\n";

                    busInfos += busInformation;
                }
                marker.setSnippet(busInfos);

            }
        }
        return busInfos;
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int idFromSwitch= buttonView.getId();

        switch (idFromSwitch){
            case R.id.busSwitch:
                showMarkersOnScreen(allBusMarkers, isChecked);
                storeBooleanValueToSharedPreferences(showBusStationPreference,isChecked);
                break;
            case R.id.velohSwitch:
                storeBooleanValueToSharedPreferences(showVelohStationPreference, isChecked);
                break;
        }
    }

    private void showMarkersOnScreen(ArrayList<Marker> markers, boolean show) {
        for(Marker marker: markers){
            marker.setVisible(show);
        }
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
        editor.commit();
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


    @Override
    public void finishedBusDownload() {
        LatLng coordinateMarker;
        Log.d(TAG,"finishedVelohDownload");
        for(BusStation busStation: allBusStations){
            coordinateMarker = new LatLng(busStation.getLatitude(),busStation.getLongitude());
            addBusMarkerFromLocation(coordinateMarker,busStation.getStationName(),busStation.getId());
        }
        Log.d(TAG,"finishedVelohDownload size "+ allBusMarkers.size());
    }

    @Override
    public void finishedVelohDownload() {
        LatLng coordinateMarker;
        Log.d(TAG,"finishedVelohDownload");
        for(VelohStation velohStation: allVelohStations){
            coordinateMarker = new LatLng(velohStation.getLatitude(),velohStation.getLongitude());
            addVelohMarkerFromLocation(coordinateMarker,velohStation.getStationName(),""+velohStation.getId());
        }
        Log.d(TAG,"finishedVelohDownload size "+ allVelohMarkers.size());
    }

    private ArrayList<BusStation> findBusStationsInRadius(LatLng currentLocation, double radius){

        ArrayList<BusStation> busStationsInRadius = new ArrayList<>();

        double distance;
        for (BusStation busStation:allBusStations){
            distance = distanceFromTwoLatLngInKM(currentLocation, busStation.getLatLng());
            if(distance<=radius){
                busStationsInRadius.add(busStation);
            }
        }

        return busStationsInRadius;
    }

    private ArrayList<VelohStation> findVelohStationsInRadius(LatLng currentLocation, double radius){

        ArrayList<VelohStation> velohStationsInRadius = new ArrayList<>();

        double distance;
        for (VelohStation velohStation:allVelohStations){
            distance = distanceFromTwoLatLngInKM(currentLocation, velohStation.getLatLng());
            if(distance<=radius){
                velohStationsInRadius.add(velohStation);
            }
        }

        return velohStationsInRadius;
    }

    private ArrayList<BusStation> findClosestBusStations(LatLng currentLocation, int amount){
        double distance1, distance2;
        ArrayList<BusStation> tempBusStations = allBusStations;
        ArrayList<BusStation> closestBusStations = new ArrayList<>();
        ArrayList<Double> distances = new ArrayList<>();
        //calculate the first (amount) of distances
        for(int i = 0 ; i < amount; i++){
            distance1 = distanceFromTwoLatLngInKM(tempBusStations.get(i).getLatLng(),currentLocation);
            distances.add(distance1);
            closestBusStations.add(tempBusStations.get(i));
        }

        //sort the distances and the corresponding busstations
        for(int i=1; i<distances.size(); i++) {
            for(int j=0; j<distances.size()-i; j++) {
                distance1 = distances.get(j);
                distance2 = distances.get(j+1);
                if(distance1>distance2) {
                    Collections.swap(distances, i, j);
                    Collections.swap(closestBusStations, i, j);
                }
            }
        }

        for(int i = amount; i<tempBusStations.size();i++){
            distance2 = distanceFromTwoLatLngInKM(tempBusStations.get(i).getLatLng(),currentLocation);
            for(int j=0; j<distances.size(); j++) {
                distance1 = distances.get(j);
                if(distance1>distance2) {
                    distances.add(j,distance2);
                    closestBusStations.add(j,tempBusStations.get(i));
                    distances.remove(amount);
                    closestBusStations.remove(amount);
                    break;
                }
            }
        }

        return closestBusStations;
    }

    private ArrayList<VelohStation> findClosestVelohStations(LatLng currentLocation, int amount){
        ArrayList<VelohStation> tempVelohStations;
        tempVelohStations = sortVelohStationsByDistance(currentLocation, allVelohStations);
        ArrayList<VelohStation> closestVelohStations = new ArrayList<>();

        for(int i = 0 ; i < amount; i++){
            closestVelohStations.add(tempVelohStations.get(i));
        }

        return closestVelohStations;
    }

    private ArrayList<BusStation> sortBusStationsByDistance(LatLng currentLocation, ArrayList<BusStation> busStations){
        BusStation temp;
        double distance1, distance2;

        //return sortBusStationsByDistance(currentLocation, (List<Station>) busStations);

        for(int i=1; i<busStations.size(); i++) {
            for(int j=0; j<busStations.size()-i; j++) {
                distance1 = distanceFromTwoLatLngInKM(busStations.get(j).getLatLng(),currentLocation);
                distance2 = distanceFromTwoLatLngInKM(busStations.get(j+1).getLatLng(),currentLocation);
                if(distance1>distance2) {
                    temp=busStations.get(j);
                    busStations.set(j,busStations.get(j+1));
                    busStations.set(j+1,temp);
                }
            }
        }
        return busStations;
    }

    private ArrayList<VelohStation> sortVelohStationsByDistance(LatLng currentLocation, ArrayList<VelohStation> velohStations){
        VelohStation temp;
        double distance1, distance2;

        for(int i=1; i<velohStations.size(); i++) {
            for(int j=0; j<velohStations.size()-i; j++) {
                distance1 = distanceFromTwoLatLngInKM(velohStations.get(j).getLatLng(),currentLocation);
                distance2 = distanceFromTwoLatLngInKM(velohStations.get(j+1).getLatLng(),currentLocation);
                if(distance1>distance2) {
                    temp=velohStations.get(j);
                    velohStations.set(j,velohStations.get(j+1));
                    velohStations.set(j+1,temp);
                }
            }
        }
        return velohStations;
    }

    private ArrayList<Station> sortStationsByDistance(LatLng currentLocation, ArrayList<Station> stations){
        Station temp;
        double distance1, distance2;

        for(int i=1; i<stations.size(); i++){
            for(int j=0; j<stations.size()-i; j++){
                distance1 = distanceFromTwoLatLngInKM(stations.get(j).getLatLng(),currentLocation);
                distance2 = distanceFromTwoLatLngInKM(stations.get(j+1).getLatLng(),currentLocation);
                if(distance1>distance2) {
                    temp=stations.get(j);
                    stations.set(j,stations.get(j+1));
                    stations.set(j+1,temp);
                }
            }
        }
        return stations;
    }



    private void showScanInRadiusAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout linear=new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView textView = new TextView(this);
        textView.setTextSize(30);
        textView.setText("1000 meter");
        textView.setGravity(Gravity.CENTER);
        linear.addView(textView);
        final SeekBar slider = new SeekBar(this);
        slider.setProgress(50);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText("" + progress*20 +" meter");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        linear.addView(slider);
        builder.setView(linear);
        builder.setTitle("Scan Stations in Radius");
        builder.setPositiveButton("Scan", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(mMap.isMyLocationEnabled()){
                    LatLng currentLocation = new LatLng(mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude());
                    double radius = (double)slider.getProgress()/(double)50;
                    ArrayList<BusStation> foundBusStations = findBusStationsInRadius(currentLocation, radius);
                    Log.d(TAG, "amount of busstations found : " +foundBusStations.size());
                    ArrayList<VelohStation> foundVelohStations = findVelohStationsInRadius(currentLocation, radius);
                    Log.d(TAG, "amount of velohStations found : " +foundVelohStations.size());
                    //TODO Show the markers of those busStations/velohStations in a better way ( other color for example)
                    zoomToLatLngWithRadius(currentLocation, radius);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.show();
    }

    private void zoomToLatLngWithRadius(LatLng currentLocation, double radius) {
        LatLng leftTop = new LatLng(currentLocation.latitude - (0.0001 * (radius /0.010)),currentLocation.longitude - (0.0001 * (radius /0.010)));
        LatLng rightBottom = new LatLng(currentLocation.latitude + (0.0001 * (radius /0.010)),currentLocation.longitude + (0.0001 * (radius /0.010)));
        zoomBetweenTwoLatLngs(leftTop,rightBottom);
    }



    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.buttonScanInRadius:
                Log.d(TAG, "Scan in radius");
                showScanInRadiusAlertDialog();
                break;
            case R.id.buttonScanNearest:
                Log.d(TAG, "Scan nearest");
                showScanNearestAlertDialog();
                break;
            case R.id.buttonSearch:
                showSearchAlertDialog();
                break;
            case R.id.buttonFavorites:
                showFavouritesDialog();
                break;
        }
        return true;
    }



    private void showScanNearestAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout linear=new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final TextView textViewBus = new TextView(this);
        textViewBus.setTextSize(25);
        textViewBus.setText("5 Bus Stations");
        textViewBus.setGravity(Gravity.CENTER);
        linear.addView(textViewBus);
        final SeekBar sliderBus = new SeekBar(this);
        sliderBus.setProgress(50);
        sliderBus.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewBus.setText("" + (progress+5)/10 +" Bus Stations");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        linear.addView(sliderBus);

        final TextView textViewVeloh = new TextView(this);
        textViewVeloh.setTextSize(25);
        textViewVeloh.setText("3 Veloh Stations");
        textViewVeloh.setGravity(Gravity.CENTER);
        linear.addView(textViewVeloh);
        final SeekBar sliderVeloh = new SeekBar(this);
        sliderVeloh.setProgress(50);
        sliderVeloh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewVeloh.setText("" + (progress+10)/20 +" Veloh Stations");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        linear.addView(sliderVeloh);
        builder.setView(linear);

        builder.setView(linear);
        builder.setTitle("Scan nearest Stations");
        builder.setPositiveButton("Scan", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(mMap.isMyLocationEnabled()){
                    setAllBusMarkerColors();
                    setAllVelohMarkerColors();
                    LatLng currentLocation = new LatLng(mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude());
                    int amountBusStations = (sliderBus.getProgress()+5)/10;
                    int amountVelohStations = (sliderVeloh.getProgress()+10)/20;
                    if(amountBusStations + amountVelohStations == 0){
                        return;
                    }
                    ArrayList<BusStation> foundBusStations = findClosestBusStations(currentLocation, amountBusStations);
                    Log.d(TAG, "amount of busstations found : " +foundBusStations.size());

                    ArrayList<VelohStation> foundVelohStations = findClosestVelohStations(currentLocation, amountVelohStations);
                    Log.d(TAG, "amount of velohStations found : " +foundVelohStations.size());

                    ArrayList<LatLng> latLngs  = new ArrayList<LatLng>();
                    for (int i = 0; i < amountBusStations; i++){
                        latLngs.add(foundBusStations.get(i).getLatLng());
                    }
                    for (int i = 0; i < amountVelohStations; i++){
                        latLngs.add(foundVelohStations.get(i).getLatLng());
                    }
                    latLngs.add(currentLocation);
                    ArrayList<LatLng> borders = getBordersOfLatLngs(latLngs);
                    //TODO Show the markers of those busStations/velohStations in a better way ( other color for example)
                    zoomBetweenTwoLatLngs(borders.get(0), borders.get(1));

                    setMarkerColorsOfBusStations(foundBusStations);
                    setMarkerColorsOfVelohStations(foundVelohStations);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        builder.show();

    }


    private ArrayList<LatLng> getBordersOfLatLngs(ArrayList<LatLng> latLngs){
        double west, north, east, south;
        west = east = latLngs.get(0).longitude;
        north = south = latLngs.get(0).latitude;
        for (int i = 1; i < latLngs.size(); i++){
            west = Math.min(west, latLngs.get(i).longitude);
            east = Math.max(east, latLngs.get(i).longitude);
            north = Math.min(north, latLngs.get(i).latitude);
            south = Math.max(south, latLngs.get(i).latitude);
        }

        ArrayList<LatLng> result = new ArrayList<>();
        result.add(new LatLng(north,west));
        result.add(new LatLng(south,east));
        return result;

    }

    private void setAllBusMarkerColors(){
        for (Marker marker: allBusMarkers){
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }
    }

    private void setAllVelohMarkerColors(){
        for (Marker marker: allVelohMarkers){
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }
    }

    private void setMarkerColorsOfBusStations(ArrayList<BusStation> busStations){
        Marker marker;
        for (BusStation busStation : busStations){
            marker = getMarkerOfBusStation(busStation);
            if(marker!=null){
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                marker.setAlpha(1);
            }
        }
    }

    private Marker getMarkerOfBusStation(BusStation busStation){
        for (Marker marker: allBusMarkers){
            if(marker.getPosition().equals(busStation.getLatLng())){
                return marker;
            }
        }
        return null;
    }

    private void setMarkerColorsOfVelohStations(ArrayList<VelohStation> velohStations){
        for (VelohStation busStation : velohStations){
            for (Marker marker: allVelohMarkers){
                if(marker.getPosition().equals(busStation.getLatLng())){
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    marker.setAlpha(1);
                }
            }
        }
    }


    private void showSearchAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout linear = new LinearLayout(this);
        linear.setOrientation(LinearLayout.VERTICAL);
        final AutoCompleteTextView textView = new AutoCompleteTextView(this);
        textView.setTextSize(20);
        textView.setHint("City, Street");
        textView.setGravity(Gravity.CENTER);

        ArrayList<String> stationNames = getStationNames();


        ArrayAdapter adapter = new
                ArrayAdapter(this, android.R.layout.simple_list_item_1, stationNames);

        textView.setAdapter(adapter);
        textView.setThreshold(1);
        textView.setDropDownHeight(800);

        linear.addView(textView);
        builder.setView(linear);
        builder.setTitle("Search for Station");
        builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                LatLng currentLocation = new LatLng(mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude());
                //TODO zoom to this location
                BusStation searchedBusStation  = getBusStationWithStationName(textView.getEditableText().toString());
                Marker busMarker = getMarkerOfBusStation(searchedBusStation);
                busMarker.showInfoWindow();
                zoomBetweenTwoLatLngs(currentLocation, searchedBusStation.getLatLng());

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });


        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();

        wmlp.gravity = Gravity.TOP | Gravity.LEFT;
        wmlp.y = 100;   //y position
        dialog.getWindow().setAttributes(wmlp);
        dialog.show();
    }


    private ArrayList<String> getStationNames() {
        ArrayList<String> stationNames = new ArrayList<>();

        for(BusStation busStation: allBusStations){
            stationNames.add(busStation.getStationName());
        }

        return stationNames;
    }


    private BusStation getBusStationWithStationName(String stationName){
        for (BusStation busStation : allBusStations){
            if (busStation.getStationName().equals(stationName)){
                return busStation;
            }
        }
        return null;
    }



    private void showFavouritesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        if (favourites.size() == 0){
            TextView textView = new TextView(this);
            textView.setText("You have no favourites!");
            textView.setTextSize(15);
            builder.setView(textView);
            builder.setTitle("Favourites");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
             dialog = builder.create();
        }else {

            LinearLayout linear = new LinearLayout(this);
            linear.setOrientation(LinearLayout.VERTICAL);
            final Spinner spinner = new Spinner(this);
            spinner.setGravity(Gravity.CENTER);


            ArrayList<String> stationNames = getStationNamesOfMarkers(favourites);


            ArrayAdapter adapter = new
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stationNames);

            spinner.setAdapter(adapter);

            linear.addView(spinner);
            builder.setView(linear);
            builder.setTitle("Favourites");
            builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    LatLng currentLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                    //TODO zoom to this location
                    BusStation searchedBusStation = getBusStationWithStationName(spinner.getSelectedItem().toString());
                    Marker busMarker = getMarkerOfBusStation(searchedBusStation);
                    busMarker.showInfoWindow();
                    zoomBetweenTwoLatLngs(currentLocation, searchedBusStation.getLatLng());

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });


            dialog = builder.create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();

            wmlp.gravity = Gravity.TOP | Gravity.LEFT;
            wmlp.y = 100;   //y position
            dialog.getWindow().setAttributes(wmlp);
        }
        dialog.show();

    }


    private ArrayList<String> getStationNamesOfMarkers(ArrayList<Marker> markers){
        ArrayList<String> stationNames = new ArrayList<>();

        for(Marker marker: markers){
            stationNames.add(marker.getTitle());
        }
        Collections.sort(stationNames);
        return stationNames;
    }

}
