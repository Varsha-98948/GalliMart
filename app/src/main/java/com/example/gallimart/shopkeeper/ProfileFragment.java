package com.example.gallimart.shopkeeper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvRole;
    private Button btnLogout;
    private MaterialCardView optionOrders, optionSaveLocation, optionHelp;
    private SessionManager sessionManager;

    private MapView mapView;
    private GeoPoint currentPoint;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_shopkeeper, container, false);

        sessionManager = new SessionManager(getContext());

        // Bind views
        ivProfile = view.findViewById(R.id.ivProfileShopkeeper);
        tvName = view.findViewById(R.id.tvNameShopkeeper);
        tvEmail = view.findViewById(R.id.tvEmailShopkeeper);
        tvRole = view.findViewById(R.id.tvRoleShopkeeper);

        optionOrders = view.findViewById(R.id.optionOrdersShopkeeper);
        optionSaveLocation = view.findViewById(R.id.optionSaveLocationShopkeeper);
        optionHelp = view.findViewById(R.id.optionHelpShopkeeper);

        btnLogout = view.findViewById(R.id.btnLogoutShopkeeper);

        // Initialize OSMDroid map
        mapView = view.findViewById(R.id.mapShopLocation);
        Configuration.getInstance().load(requireContext(),
                android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        loadUserDetails();
        runFadeInAnimations();
        setupClicks();

        // Show existing shop location if already saved
        showSavedShopLocation();

        return view;
    }

    private void loadUserDetails() {
        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
        tvRole.setText("Shopkeeper");
    }

    private void setupClicks() {
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout(true);
            startActivity(new android.content.Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });

        optionOrders.setOnClickListener(v -> {
            String shopId = sessionManager.getShopId();
            if (shopId == null || shopId.isEmpty()) {
                Toast.makeText(getContext(), "Save your shop location first!", Toast.LENGTH_SHORT).show();
                return;
            }

            InventoryFragment inventoryFragment = new InventoryFragment();
            Bundle bundle = new Bundle();
            bundle.putString("shopId", shopId);
            inventoryFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, inventoryFragment)
                    .addToBackStack(null)
                    .commit();
        });

        optionHelp.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Help & Support")
                    .setMessage("Contact us at:\ngallimart.contact@gmail.com")
                    .setPositiveButton("OK", null)
                    .show();
        });

        optionSaveLocation.setOnClickListener(v -> saveCurrentLocation());
    }

    private void runFadeInAnimations() {
        int delay = 100;
        fadeInView(ivProfile, delay);
        fadeInView(tvName, delay + 200);
        fadeInView(tvEmail, delay + 400);
        fadeInView(tvRole, delay + 600);
        fadeInView(optionOrders, delay + 800);
        fadeInView(optionSaveLocation, delay + 1000);
        fadeInView(optionHelp, delay + 1200);
        fadeInView(btnLogout, delay + 1400);
    }

    private void fadeInView(View view, int delay) {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setStartOffset(delay);
        fadeIn.setDuration(400);
        view.startAnimation(fadeIn);
        view.setVisibility(View.VISIBLE);
    }

    // ==================== LOCATION HANDLING ====================
    private void saveCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        android.location.LocationManager lm = (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);

        if (location != null) {
            currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            saveShopLocation(currentPoint.getLatitude(), currentPoint.getLongitude());
            updateMapMarker(currentPoint);
        } else {
            Toast.makeText(getContext(), "Unable to get current location. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveShopLocation(double lat, double lng) {
        String shopId = sessionManager.getShopId();
        if (shopId == null || shopId.isEmpty()) {
            shopId = FirebaseDatabase.getInstance().getReference("shops").push().getKey();
            sessionManager.setShopId(shopId);
        }

        DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference("shops").child(shopId);
        shopRef.child("lat").setValue(lat);
        shopRef.child("lng").setValue(lng);
        shopRef.child("shopId").setValue(shopId);
        shopRef.child("name").setValue(sessionManager.getUserName());
        shopRef.child("email").setValue(sessionManager.getUserEmail());

        Toast.makeText(getContext(), "Shop location saved!", Toast.LENGTH_SHORT).show();
    }

    private void showSavedShopLocation() {
        String shopId = sessionManager.getShopId();
        if (shopId != null && !shopId.isEmpty()) {
            DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference("shops").child(shopId);
            shopRef.get().addOnSuccessListener(snapshot -> {
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lng = snapshot.child("lng").getValue(Double.class);
                if (lat != null && lng != null) {
                    GeoPoint point = new GeoPoint(lat, lng);
                    updateMapMarker(point);
                }
            });
        }
    }

    private void updateMapMarker(GeoPoint point) {
        mapView.getOverlays().clear();
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle("Your Shop");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.getController().setCenter(point);
        mapView.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
