package com.example.gallimart.shopkeeper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private Button btnLogout, btnSaveLocation;
    private SessionManager sessionManager;
    private MapView mapView;
    private GeoPoint currentPoint;

    @com.example.gallimart.shopkeeper.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_shopkeeper, container, false);

        sessionManager = new SessionManager(getContext());

        tvName = view.findViewById(R.id.tvNameShopkeeper);
        tvEmail = view.findViewById(R.id.tvEmailShopkeeper);
        tvRole = view.findViewById(R.id.tvRoleShopkeeper);
        btnLogout = view.findViewById(R.id.btnLogoutShopkeeper);
        btnSaveLocation = view.findViewById(R.id.btnSaveLocationShopkeeper);

        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
        tvRole.setText("Shopkeeper");

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // OSMDroid setup
        Configuration.getInstance().load(requireContext(),
                android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));

        mapView = view.findViewById(R.id.mapShopLocation);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        // Marker for shop
        Marker shopMarker = new Marker(mapView);
        shopMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(shopMarker);

        // Location permission & FusedLocationProviderClient
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        } else {
            FusedLocationProviderClient fusedClient =
                    LocationServices.getFusedLocationProviderClient(requireContext());
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().setCenter(currentPoint);
                    shopMarker.setPosition(currentPoint);
                    shopMarker.setTitle("Your shop here");
                    mapView.invalidate();
                }
            });
        }

        btnSaveLocation.setOnClickListener(v -> {
            if (currentPoint != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("shops")
                        .child(sessionManager.getUserName());
                ref.child("name").setValue(sessionManager.getUserName());
                ref.child("email").setValue(sessionManager.getUserEmail());
                ref.child("lat").setValue(currentPoint.getLatitude());
                ref.child("lng").setValue(currentPoint.getLongitude());
                Toast.makeText(getContext(), "Shop location saved!", Toast.LENGTH_SHORT).show();
            }
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
