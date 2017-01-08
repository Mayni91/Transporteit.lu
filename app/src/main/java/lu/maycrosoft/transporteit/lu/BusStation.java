package lu.maycrosoft.transporteit.lu;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * The BusStation class inherits again the main properties of the Station class and
 * adds the method getBusInformations, which collects all the bus information for this specific
 * busStation and calls the AsyncTask which handles the gathering of this information
 *
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
