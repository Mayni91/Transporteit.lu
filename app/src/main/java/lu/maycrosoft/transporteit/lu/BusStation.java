package lu.maycrosoft.transporteit.lu;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joemayer on 10/12/2016.
 */

public class BusStation {

    private String id;
    private String stationName;
    private double latitude;
    private double longitude;

    public BusStation(String id, String stationName, double latitude, double longitude) {
        this.id = id;
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
        Location loc = new Location(getStationName());
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

    public String getId() {
        return id;
    }

}
