package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ShopsMapFragment extends Fragment {

    private MapView mapView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragments_shop_map, container, false);

        Configuration.getInstance().load(requireContext(),
                android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));

        mapView = view.findViewById(R.id.mapViewShops);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(28.6139, 77.2090)); // Default center (Delhi)

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("shops");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double lat = ds.child("lat").getValue(Double.class);
                    Double lng = ds.child("lng").getValue(Double.class);
                    String name = ds.child("name").getValue(String.class);
                    if (lat != null && lng != null) {
                        GeoPoint point = new GeoPoint(lat, lng);
                        Marker marker = new Marker(mapView);
                        marker.setPosition(point);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setTitle(name);
                        marker.setOnMarkerClickListener((m, map) -> {
                            Toast.makeText(getContext(), "Clicked: " + name, Toast.LENGTH_SHORT).show();
                            return true;
                        });
                        mapView.getOverlays().add(marker);
                    }
                }
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
