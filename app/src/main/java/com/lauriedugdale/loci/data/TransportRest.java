package com.lauriedugdale.loci.data;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;
import com.lauriedugdale.loci.data.dataobjects.BusStop;
import com.lauriedugdale.loci.ui.adapter.nearme.BusStopsAdapter;
import com.lauriedugdale.loci.utils.LocationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


/**
 * Created by mnt_x on 31/07/2017.
 */

public class TransportRest {

    private static final String TAG = TransportRest.class.getSimpleName();
    private static final String APP_ID= "53298a9e";
    private static final String APP_KEY ="1c00dc64872877d2a53cf10c553d3cb8";

    public interface BusStopsDownloadedListener{
        void onBusstopsDownloaded();
    }

    public void getBusStops(final Activity activity, final double currentLatitude, final double currentLongitude, final double radius, final BusStopsAdapter adapter, final BusStopsDownloadedListener listener) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                LatLngBounds bounds = LocationUtils.toBounds(currentLatitude, currentLongitude, radius);

                String url = "https://transportapi.com/v3/uk/bus/stops/bbox.json?" +
                            "app_id="   + APP_ID +
                            "&app_key=" + APP_KEY  +
                            "&maxlat="  + bounds.southwest.latitude +
                            "&maxlon="  + bounds.southwest.longitude +
                            "&minlat="  + bounds.northeast.latitude +
                            "&minlon="  + bounds.northeast.longitude;

                // Making a request to url and getting response
                String jsonString = makeServiceCall(url);
                if (jsonString != null) {
                    try {
                        JSONObject jsonObj = new JSONObject(jsonString);

                        // Getting JSON Array node
                        JSONArray stops = jsonObj.getJSONArray("stops");

                        // looping through stops
                        for (int i = 0; i < stops.length(); i++) {

                            JSONObject c = stops.getJSONObject(i);

                            final String atcocode = c.getString("atcocode");
                            final String name = c.getString("name");
                            final String locality = c.getString("locality");
                            final double latitude = c.getDouble("latitude");
                            final double longitude = c.getDouble("longitude");
                            final float distance = LocationUtils.getDistanceInMeters(latitude, longitude, currentLatitude, currentLongitude);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.addToStops(new BusStop(atcocode, name, locality, latitude, longitude, distance));
                                    listener.onBusstopsDownloaded();
                                }
                            });

                        }
                    } catch ( JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());


                    }
                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                }
            }
        });
    }


    public String makeServiceCall(String reqUrl) {
        String response = null;
        try {
            URL url = new URL(reqUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            response = convertStreamToString(in);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "ProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }

        return response;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
