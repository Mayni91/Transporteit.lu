package lu.maycrosoft.transporteit.lu;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by joemayer on 23/11/2016.
 */

public class JsonFileHandler {

    public void writeJsonToInternalStorage(JSONObject obj, Context context){
        String FILENAME = "bus_stations.json";

        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(obj.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void loadJsonDataFrom(){
        Calendar cal = Calendar.getInstance();
        Date currentTime = cal.getTime();
        System.out.println(currentTime);

        try {
            URL url  = new URL("http://travelplanner.mobiliteit.lu/hafas/query.exe/dot?performLocating=2&tpl=stop2csv&look_maxdist=150000&look_x=6112550&look_y=49610700&stationProxy=yes" );
            URLConnection uc = url.openConnection();

            InputStreamReader inStream = new InputStreamReader(uc.getInputStream(), "UTF8");
            BufferedReader buff= new BufferedReader(inStream);

            String content = null;

            JSONObject objStation = new JSONObject();
            JSONArray jsonStations = new JSONArray();
            JSONObject obj = new JSONObject();

            while ((content = buff.readLine()) != null) {

                objStation = encodeStationsToJSON(content);
                jsonStations.put(objStation);

            }
            obj.put("stations", jsonStations);

        }
        catch (Exception e){
            System.out.print("Exception");
        }

        System.out.println("done");
        cal = Calendar.getInstance();
        currentTime = cal.getTime();
        System.out.println(currentTime);
    }


    public JSONObject encodeStationsToJSON(String station){
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


}
