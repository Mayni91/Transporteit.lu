package lu.maycrosoft.transporteit.lu;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * This class is the parent class of the veloh and bus station classes, since both classes share a lot of common properties
 */

public abstract class Station {

    private String stationName;
    private double latitude;
    private double longitude;

    public Station(String stationName, double latitude, double longitude) {
        this.stationName = stationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public LatLng getLatLng(){
        return new LatLng(getLatitude(),getLongitude());
    }

    private Location getLocation(){
        return getLocation("");
    }

    private Location getLocation(String locationName){
        Location loc = new Location(locationName);
        loc.setLatitude(getLatitude());
        loc.setLongitude(getLongitude());
        return loc;
    }

    public float getDistanceInMetersFromCurrentLocationToBusStation(Location currentLocation){
        return currentLocation.distanceTo(getLocation());
    }

    public String getStationName() {
        return stationName;
    }



}
