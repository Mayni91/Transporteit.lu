package lu.maycrosoft.transporteit.lu;

import java.util.ArrayList;

/**
 * This class handles the structure of the bus information, it has a busline, times and a destination name
 *
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
