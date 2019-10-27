package com.oober.oober;

import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.RoadsApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.SnappedPoint;
import com.google.maps.model.TravelMode;

import java.util.Iterator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final double[] RIMAC_LATLNG = new double[]{32.885251, -117.239186};
    private static final double[] JUSTICE_LN_LATLNG = new double[]{32.883128, -117.232217};
    private static final double SEARCH_RADIUS = (15.0 + 12.0) / 2.0;
    private static final String GEO_API_KEY = "AIzaSyAC05OOvIYZ1pkOu2ePkoSlHDBno-8g3QA";
    private static final String MOOBER_FILE = "images\\moober.bmp";
    private static final double COW_WALK_MPH = 50.0;
    private static final float ROUTE_WIDTH = 10.0f;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float ZOOM_LEVEL = 16.0f; //This goes up to 21

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

        // TODO just a test
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context,
                    "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Log.d("Geocoding result", gson.toJson(results[0].addressComponents));
        } catch (Exception e) {
            Log.d("Geocoding error", e.getMessage());
        }

        // Create button to call oober

    }

    public void requestOober(View v) {
        // Choose random point for driver
        double radius = (milesToLatDeg(SEARCH_RADIUS) +
                milesToLngDeg(currentLoc.latitude, SEARCH_RADIUS)) / 2.0;
        // TODO make random point radius
        com.google.maps.model.LatLng driverPnt = new com.google.maps.model.LatLng(
                JUSTICE_LN_LATLNG[0], JUSTICE_LN_LATLNG[1]);

        Log.d("driverPnt", "Lat: " + driverPnt.lat + " Lng: " + driverPnt.lng);


        SnappedPoint[] roadResults = null;
        try {
            roadResults = RoadsApi.nearestRoads(
                    context, driverPnt).await();
            Log.d("RoadsApi result", GSON.toJson(roadResults[0]));
        } catch (Exception e) {
            Log.d("RoadsApi error", e.getMessage());
        }

        Marker driverMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(roadResults[0].location.lat, roadResults[0].location.lng))
                .title("Moober")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spoiler_moober))
                .anchor(0.5f, 0.5f));

        DirectionsApiRequest request = DirectionsApi.newRequest(context)
                .destination(new com.google.maps.model.LatLng(
                        currentLoc.latitude, currentLoc.longitude))
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(
                        driverMarker.getPosition().latitude,
                        driverMarker.getPosition().longitude))
                .optimizeWaypoints(true);

        DirectionsResult dirResult = generateDirResult(request);

        Log.d("DirectionsResult", Integer.toString(dirResult.routes.length));

        Polyline route = renderRoute(dirResult.routes[0]);
        route.setVisible(false);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(driverPnt.lat, driverPnt.lng), ZOOM_LEVEL));

        // TODO animate marker
        animateMarker(driverMarker, route);

        //plotWaypoints(route);

    }

    private DirectionsResult generateDirResult(DirectionsApiRequest request) {
        DirectionsResult dirResult = null;
        try {
            dirResult = request.await();
            Log.d("DirectionsApi result", GSON.toJson(dirResult));
        } catch (Exception e) {
            Log.d("DirectionsApi error", e.getMessage());
        }
        return dirResult;
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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, ZOOM_LEVEL));
    }

    private Polyline renderRoute(DirectionsRoute route) {
        PolylineOptions lineOpts = new PolylineOptions();
        for (com.google.maps.model.LatLng waypoint : route.overviewPolyline.decodePath()) {
            lineOpts.add(new LatLng(waypoint.lat, waypoint.lng));
        }
        return mMap.addPolyline(lineOpts
                .color(Color.BLUE)
                .width(ROUTE_WIDTH));
    }

    private Polyline renderRoute(LatLng start, LatLng end) {
        return mMap.addPolyline(new PolylineOptions()
                .add(start)
                .add(end)
                .color(Color.BLUE)
                .width(ROUTE_WIDTH));
    }

    private void animateMarker(Marker m, Polyline route) {
        Iterator<LatLng> iter = route.getPoints().iterator();
        animateMarkerHelper(m, iter, 0, route.getPoints().get(0));
    }

    private void animateMarkerHelper(Marker m, Iterator<LatLng> iter, int multiplier, LatLng prev) {
        if (!iter.hasNext()) { return; }
        final Handler handler = new Handler();
        LatLng waypoint = iter.next();
        Polyline route = renderRoute(prev, waypoint);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MarkerAnimation.animateMarkerToGB(m, waypoint, new LatLngInterpolator.Spherical());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        route.setVisible(false);
                    }
                }, 1000);
            }
        }, 2000 * multiplier);
        animateMarkerHelper(m, iter, ++multiplier, waypoint);
    }

    private void plotWaypoints(Polyline route) {
        Iterator<LatLng> iter = route.getPoints().iterator();
        plotWaypointsHelper(iter, 0);
    }

    private void plotWaypointsHelper(Iterator<LatLng> iter, int multiplier) {
        if (!iter.hasNext()) { return; }
        final Handler handler = new Handler();
        LatLng waypoint = iter.next();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMap.addMarker(new MarkerOptions().position(waypoint));
            }
        }, 100 * multiplier);
        plotWaypointsHelper(iter, ++multiplier);
    }



    /**
     * Picks a random point within the radius of a starting point.
     * @param lat Latitude of starting point
     * @param lon Longitude of starting point
     * @param radius Radius around starting point
     * @return double array of size 2, contains latitude and longitude of random point
     */
    private double[] pickRandomPoint(double lat, double lon, double radius) {
        double newRadiusLon= milesToLatDeg(radius);
        double newRadiusLat= milesToLngDeg(currentLoc.latitude,radius);
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

    private static double distance(LatLng point1, LatLng point2) {
        double distances = Math.pow(point1.latitude-point2.latitude,2) + Math.pow(point1.longitude-point2.longitude,2);
        distances = Math.sqrt(distances);
        return distances;
    }
    //For the cow names
    private static String mooberName() {
        String[] names = new String[]{"Fred","Molly","Joe", "Bob"};
        return names[(int)(Math.random()*names.length)];
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.shutdown();
    }
}
