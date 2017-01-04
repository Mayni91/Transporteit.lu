package lu.maycrosoft.transporteit.lu;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joemayer on 10/12/2016.
 */

public class BusStation extends Station{

    private String id;

    public BusStation(String id, String stationName, double latitude, double longitude) {
        super(stationName, latitude, longitude);
        this.id = id;

    }

    public String getId() {
        return id;
    }

}
