package lu.maycrosoft.transporteit.lu;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joemayer on 13/12/2016.
 */

public class VelohStation {
    private int id;
    private String stationName;
    private String address;
    private double latitude;
    private double longitude;

    public VelohStation(int id, String stationName, String address, double latitude, double longitude) {
        this.id = id;
        this.stationName = stationName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public String getStationName() {
        return stationName;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LatLng getLatLng(){
        return new LatLng(getLatitude(),getLongitude());
    }
}
