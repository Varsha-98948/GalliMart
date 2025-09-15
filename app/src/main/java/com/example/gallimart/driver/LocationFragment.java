package com.example.gallimart.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.preference.PreferenceManager;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.driver.adapters.DriverLocationAdapter;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class LocationFragment extends Fragment {

    private RecyclerView rvDriverLocations;
    private DriverLocationAdapter adapter;
    private List<DriverLocation> locationList;
    private MapView mapView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_driver, container, false);

        // Required for OSMDroid to load/cache tiles
        Configuration.getInstance().load(getContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set default view (lat, lon, zoom)
        GeoPoint startPoint = new GeoPoint(28.6139, 77.2090); // Example: Delhi
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(startPoint);

        // Add a marker
        Marker marker = new Marker(mapView);
        marker.setPosition(startPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Sample Shop Location");
        mapView.getOverlays().add(marker);

        rvDriverLocations = view.findViewById(R.id.rvDriverLocations);
        rvDriverLocations.setLayoutManager(new LinearLayoutManager(getContext()));

        // Sample driver locations data
        locationList = new ArrayList<>();
        locationList.add(new DriverLocation("Pickup: Shop A", "Delivery: Customer X"));
        locationList.add(new DriverLocation("Pickup: Shop B", "Delivery: Customer Y"));
        locationList.add(new DriverLocation("Pickup: Shop C", "Delivery: Customer Z"));

        adapter = new DriverLocationAdapter(locationList);
        rvDriverLocations.setAdapter(adapter);

        // Optionally: get device location and move map
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // You can use FusedLocationProviderClient here to get current lat/lng and center map
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume(); // needed for compass, my location overlays, etc
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
