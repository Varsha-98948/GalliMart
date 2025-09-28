package com.example.gallimart.buyer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class AddressFragment extends Fragment {

    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private EditText etAddress;
    private Button btnSave;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference usersRef;
    private Marker centerMarker;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_address, container, false);

        etAddress = view.findViewById(R.id.etAddress);
        btnSave = view.findViewById(R.id.btnSaveAddress);
        mapView = view.findViewById(R.id.mapView);

        // osmdroid config
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Create center marker (visual anchor)
        centerMarker = new Marker(mapView);
        centerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(centerMarker);

        // Wait until layout ready → then init
        mapView.post(() -> {
            fetchCurrentLocationAndInitPin();
            attachMapTouchListener();
        });

        btnSave.setOnClickListener(v -> saveAddress());

        return view;
    }

    private void fetchCurrentLocationAndInitPin() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            setMarkerToMapCenter();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().setZoom(17.0);
                mapView.getController().setCenter(currentPoint);
                setMarkerToMapCenter();
                mapView.invalidate();
            } else {
                setMarkerToMapCenter();
                Toast.makeText(getContext(),
                        "Could not fetch GPS location; pan map to choose location.",
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            setMarkerToMapCenter();
            Toast.makeText(getContext(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setMarkerToMapCenter() {
        mapView.post(() -> {
            try {
                IGeoPoint gp = mapView.getProjection()
                        .fromPixels(mapView.getWidth() / 2, mapView.getHeight() / 2);
                GeoPoint center = new GeoPoint(gp.getLatitude(), gp.getLongitude());
                centerMarker.setPosition(center);
                mapView.invalidate();
            } catch (Exception ex) {
                centerMarker.setPosition(new GeoPoint(0.0, 0.0));
            }
        });
    }

    private void attachMapTouchListener() {
        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.post(() -> {
                    try {
                        IGeoPoint gp = mapView.getProjection()
                                .fromPixels(mapView.getWidth() / 2, mapView.getHeight() / 2);
                        GeoPoint center = new GeoPoint(gp.getLatitude(), gp.getLongitude());
                        centerMarker.setPosition(center);
                        mapView.invalidate();
                    } catch (Exception ignored) {
                    }
                });
            }
            return false;
        });
    }

    private void saveAddress() {
        String addressText = etAddress.getText().toString().trim();
        if (TextUtils.isEmpty(addressText)) {
            Toast.makeText(getContext(), "Enter your address", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        double lat, lng;
        try {
            IGeoPoint gp = mapView.getProjection()
                    .fromPixels(mapView.getWidth() / 2, mapView.getHeight() / 2);
            lat = gp.getLatitude();
            lng = gp.getLongitude();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not read map center coordinates", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ update only address field inside the user node
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("address/addressText", addressText);
        updateMap.put("address/lat", lat);
        updateMap.put("address/lng", lng);

        usersRef.child(user.getUid()).updateChildren(updateMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Address saved!", Toast.LENGTH_SHORT).show();
                    // ✅ close this fragment after saving
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocationAndInitPin();
            } else {
                Toast.makeText(getContext(),
                        "Location permission required to auto-center map.",
                        Toast.LENGTH_SHORT).show();
                setMarkerToMapCenter();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
