package lu.maycrosoft.transporteit.lu;

import com.google.android.gms.maps.model.LatLng;

/**
 *
 * This class handles the structure of the  veloh stations, takes the main properties of the parent class Station and adds an id and an address
 *
 */

public class VelohStation extends Station{
    private int id;
    private String address;

    public VelohStation(int id, String stationName, String address, double latitude, double longitude) {
        super(stationName, latitude, longitude);
        this.id = id;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }


}
