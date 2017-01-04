package lu.maycrosoft.transporteit.lu;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joemayer on 13/12/2016.
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
