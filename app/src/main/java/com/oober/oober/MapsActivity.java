package com.oober.oober;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        LatLng rimac = new LatLng(32.885251, -117.239186);
        mMap.addMarker(new MarkerOptions().position(rimac).title("Marker in RIMAC Arena"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(rimac));
    }

    /**
     * Picks a random point within the radius of a starting point.
     * @param lat Latitude of starting point
     * @param lon Longitude of starting point
     * @param radius Radius around starting point
     * @return double array of size 2, contains latitude and longitude of random point
     */
    private double[] pickRandomPoint(double lat, double lon, double radius) {
        double latDegree = Math.cos
      //Length of 1 degree of Longitude = cosine (latitude in decimal degrees) * length of degree (miles) at equator
        double randomLat = (Math.random()*(2*radius)+lat-radius);
        double randomLon = (Math.random()*(2*radius)+lon-radius);
        return new double[]{randomLon,randomLat};
    }
}
