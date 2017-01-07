package lu.maycrosoft.transporteit.lu;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

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


    public ArrayList<BusInformation> getBusInformations(){
        NextStationsURLHandler nextStationsURLHandler =  new NextStationsURLHandler(getId());
        ArrayList<BusInformation> busInformations = new ArrayList<>();

        try {
            busInformations = nextStationsURLHandler.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return busInformations;
    }

}
