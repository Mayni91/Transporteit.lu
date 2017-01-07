package lu.maycrosoft.transporteit.lu;

import java.util.ArrayList;

/**
 * Created by joemayer on 13/12/2016.
 */

public class BusInformation {

    private String id;
    private String busLine;
    private ArrayList<String> times;
    private String destinationStationName;

    public BusInformation(String busLine, ArrayList<String> times, String destinationStationName) {
        this.id = busLine + " " + destinationStationName;
        this.busLine = busLine;
        this.times = times;
        this.destinationStationName = destinationStationName;
    }

    public String getId() {
        return id;
    }

    public String getBusLine() {
        return busLine;
    }

    public ArrayList<String> getTimes() {
        return times;
    }

    public void setTimes(ArrayList<String> times) {
        this.times = times;
    }

    public String getDestinationStationName() {
        return destinationStationName;
    }
}
