package lu.maycrosoft.transporteit.lu;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Class which handles all the initial loading of the veloh- and busstations, it extends to AsyncTask because every network connection should
 * be done this way, such that the device does not completely block and rests responsive.
 */

public class JsonURLHandler extends AsyncTask<Void, Void, String> {

    private JSONObject obj = new JSONObject();
    private String busURL = "http://travelplanner.mobiliteit.lu/hafas/query.exe/dot?performLocating=2&tpl=stop2csv&look_maxdist=150000&look_x=6112550&look_y=49610700&stationProxy=yes";
    private String velohURL = "https://developer.jcdecaux.com/rest/vls/stations/Luxembourg.json";

    private String URL;

    private List<FinishedDownloadListener> listeners = new ArrayList<>();

    public JsonURLHandler(String URL){
        this.URL = URL;
    }

    public void writeJsonToInternalStorage(JSONObject obj, Context context){
        String FILENAME = "bus_stations.json";

        FileOutputStream fos = null;
        try {
            File inFile = new File(FILENAME);
            Scanner sc = new Scanner(inFile);
            fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            while(sc.hasNextLine()){
                fos.write(sc.nextLine().getBytes());
            }

            //fos.write(obj.toString().getBytes());
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private JSONObject encodeStationsToJSON(String station){
        String[] parts = station.split("\\@");

        JSONObject obj = new JSONObject();
        try {
            obj.put("id", station.substring(3).replace(";", "").trim());
            obj.put("stationName", parts[1].substring(2));
            obj.put("lat", parts[3].substring(2).replace(',', '.'));
            obj.put("long", parts[2].substring(2).replace(',', '.'));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    @Override
    protected String doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String content = "";
        URL url;
        try {
            // Construct the URL for the OpenWeatherMap query
            if(URL.equals("VELOH")){
                url = new URL(velohURL);

                content += "{\"stations\":";
            }
            else{
                url = new URL(busURL);
            }


            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));

            String line;

            switch (URL) {
                case "BUS":
                    JSONObject objStation;
                    JSONArray jsonStations = new JSONArray();


                    while ((line = reader.readLine()) != null) {

                        objStation = encodeStationsToJSON(line);
                        jsonStations.put(objStation);

                    }
                    obj.put("stations", jsonStations);
                    content = obj.toString();
                    break;

                case "VELOH":
                    while ((line = reader.readLine()) != null) {

                        content += line;

                    }
                    content += "}";
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
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
        return content;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.i("json", "onPostExecute "+s);

        if(URL.equals("VELOH")){
            for (FinishedDownloadListener hl : listeners) {
                hl.finishedVelohDownload();
            }
        }
        else{
            for (FinishedDownloadListener hl : listeners) {
                hl.finishedBusDownload();
            }
        }


    }

    public void addListener(FinishedDownloadListener listener) {
        listeners.add(listener);
    }

}
