package lu.maycrosoft.transporteit.lu;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class handles all connections in the background which are called when the user interacts with a marker,
 * such that he can be shown all the concerned information of the selected busstation.
 * This information is called over tha API url with a given ID of a station.
 * This is again done by an AsyncTask such that we can assure that the application stays responsive.
 *
 */

public class NextStationsURLHandler extends AsyncTask<Void, Void, ArrayList<BusInformation>> {

    private String id;

    public NextStationsURLHandler(String id){
        this.id =  id;
    }

    @Override
    protected ArrayList<BusInformation> doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        ArrayList<BusInformation> busInformations = new ArrayList<>();
        String content = "";

        try {
            id=id.replaceAll(" ","%20");
            URL url = new URL("http://travelplanner.mobiliteit.lu/restproxy/departureBoard?accessId=cdt&format=json&id="+id);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            Log.d("ERROR", inputStream.toString());
            if (inputStream == null) {
                // Nothing to do.
                Log.i("INPUT", "Test");

                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));

            String line;


            while ((line = reader.readLine()) != null) {

                content += line;

            }

            busInformations = getBusInformationFromJSON(content);

        } catch(FileNotFoundException exception) {
            if( busInformations == null){

                ArrayList<String> times = new ArrayList<>();
                times.add("No Information");
                BusInformation busInfo = new BusInformation("No Information",times,"No Information");
                busInformations.add(busInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        return busInformations;
    }

    private ArrayList<BusInformation> getBusInformationFromJSON(String json){

        ArrayList<BusInformation> busInformations = new ArrayList<>();
        ArrayList<BusInformation> tempBusInformations = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has("Departure")) {
                JSONArray listOfBusInformation = (JSONArray) jsonObj.get("Departure");

                for (int i = 0; i < listOfBusInformation.length(); i++) {
                    ArrayList<String> times = new ArrayList<>();
                    JSONObject busInformation = listOfBusInformation.getJSONObject(i);
                    times.add((String) busInformation.get("time"));
                    BusInformation busInfo = new BusInformation((String) busInformation.get("name"),
                            times,
                            (String) busInformation.get("direction"));
                    tempBusInformations.add(busInfo);

                }

                ArrayList<String> times = new ArrayList<>();
                busInformations = tempBusInformations;

                for (int i = 0; i < busInformations.size(); i++) {
                    for (int j = i + 1; j < tempBusInformations.size(); j++) {
                        if (busInformations.get(i).getBusLine().equals(tempBusInformations.get(j).getBusLine())
                                && busInformations.get(i).getDestinationStationName().equals(tempBusInformations.get(j).getDestinationStationName())) {

                            times.add(tempBusInformations.get(j).getTimes().get(0));
                            busInformations.remove(j);
                        }
                    }
                    busInformations.get(i).setTimes(times);
                }

                for (int i = 0; i < busInformations.size(); i++) {
                    Collections.sort(busInformations.get(i).getTimes().subList(0, busInformations.get(i).getTimes().size()));
                }
            }
            else{
                ArrayList<String> times = new ArrayList<>();
                times.add("");
                BusInformation busInfo = new BusInformation("",times,"");
                busInformations.add(busInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();

        }

        Map<String, BusInformation> map = new LinkedHashMap<>();
        for (BusInformation businfo : busInformations) {
            map.put(businfo.getId(), businfo);
        }
        busInformations.clear();
        busInformations.addAll(map.values());

        return busInformations;
    }
}
