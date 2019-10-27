package com.oober.oober;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GaeRequestHandler;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.RoadsApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.SnappedPoint;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final double[] RIMAC_LATLNG = new double[]{32.885251, -117.239186};
    private static final double SEARCH_RADIUS = (15.0 + 12.0) / 2.0;
    private static final String GEO_API_KEY = "AIzaSyAC05OOvIYZ1pkOu2ePkoSlHDBno-8g3QA";
    private static final String MOOBER_FILE = "images/moober.png";

    private GoogleMap mMap;
    private LatLng currentLoc;
    private GeoApiContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        context = new GeoApiContext.Builder()
                .apiKey(GEO_API_KEY).build();
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context,
                    "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Log.d("Geocoding result", gson.toJson(results[0].addressComponents));
        } catch (Exception e) {
            Log.d("Geocoding error", e.getMessage());
        }

        // Create button to call oober
        Button callButton = new Button(this);
        callButton.setText("Request Oober");
        addContentView(callButton, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Choose random point for driver
                double radius = (milesToLatDeg(SEARCH_RADIUS) +
                                 milesToLngDeg(currentLoc.latitude, SEARCH_RADIUS)) / 2.0;
                LatLng driver = makePoint(pickRandomPoint(currentLoc.latitude,
                                                          currentLoc.longitude, radius));
                com.google.maps.model.LatLng driverPnt = new com.google.maps.model.LatLng(
                        32.885251, -117.239186); // TODO

                Log.d("driverPnt", "Lat: " + driverPnt.lat + " Lng: " + driverPnt.lng);

                SnappedPoint[] results = null;
                try {
                    results = RoadsApi.nearestRoads(
                            context, driverPnt).await();
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Log.d("RoadsApi result", gson.toJson(results[0]));
                } catch (Exception e) {
                    Log.d("RoadsApi error", e.getMessage());
                }

                // TODO: display
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(results[0].location.lat, results[0].location.lng))
                        .title("Moober")
                        /*.icon(BitmapDescriptorFactory.fromFile(MOOBER_FILE))*/);
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        currentLoc = makePoint(RIMAC_LATLNG);
        mMap.addMarker(new MarkerOptions().position(currentLoc).title("Current Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));
    }

    /**
     * Picks a random point within the radius of a starting point.
     * @param lat Latitude of starting point
     * @param lon Longitude of starting point
     * @param radius Radius around starting point
     * @return double array of size 2, contains latitude and longitude of random point
     */
    private double[] pickRandomPoint(double lat, double lon, double radius) {
        double newRadiusLon= milesToLataDeg(currentLoc.latitude, radius);
        double newRadiusLat= milesToLngDeg(lat,radius);
        //Length of 1 degree of Longitude = cosine (latitude in decimal degrees) * length of degree (miles) at equator
        double randomLat = (Math.random()*(2*newRadiusLat)+lat-newRadiusLat);
        double randomLon = (Math.random()*(2*newRadiusLon)+lon-newRadiusLon);
        return new double[]{randomLon,randomLat};
    }

    private static LatLng makePoint(double[] coords) {
        return new LatLng(coords[0], coords[1]);
    }

    /**
     * Converts miles travelled to degrees latitude travelled.
     * @param miles Miles travelled
     * @return Degrees latitude travelled
     */
    private static double milesToLatDeg(double miles) {
        double latKM = miles * 0.621371;
        return (1 / 110.54) * latKM;
    }

    /**
     * Converts miles travelled to degrees longitude travelled.
     * @param miles Miles travelled
     * @return Degrees longitude travelled
     */
    private static double milesToLngDeg(double currentLat, double miles) {
        double longKM = miles * 0.621371;
        return (1 / (111.320 * Math.cos(currentLat))) * longKM;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.shutdown();
    }
}
