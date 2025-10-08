package com.example.gallimart.shopkeeper;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvRole;
    private Button btnLogout;
    private MaterialCardView optionOrders, optionSaveLocation, optionHelp;
    private SessionManager sessionManager;

    private DatabaseReference shopRef;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_shopkeeper, container, false);

        sessionManager = new SessionManager(getContext());

        ivProfile = view.findViewById(R.id.ivProfile);
        tvName = view.findViewById(R.id.tvNameShopkeeper);
        tvEmail = view.findViewById(R.id.tvEmailShopkeeper);
        tvRole = view.findViewById(R.id.tvRoleShopkeeper);

        optionOrders = view.findViewById(R.id.optionOrdersShopkeeper);
        optionSaveLocation = view.findViewById(R.id.optionSaveLocationShopkeeper);
        optionHelp = view.findViewById(R.id.optionHelpShopkeeper);

        btnLogout = view.findViewById(R.id.btnLogoutShopkeeper);

        loadUserDetails();
        runFadeInAnimations(view);

        setupClicks();

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
            // Navigate to ShopkeeperOrdersFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new OrdersFragment())
                    .addToBackStack(null)
                    .commit();
        });

        optionHelp.setOnClickListener(v -> {
            // Show help dialog
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Help & Support")
                    .setMessage("Contact us at:\ngallimart.contact@gmail.com")
                    .setPositiveButton("OK", null)
                    .show();
        });

        optionSaveLocation.setOnClickListener(v -> showShopLocationDialog());
    }

    private void runFadeInAnimations(View root) {
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

    private void showShopLocationDialog() {
        String shopId = sessionManager.getShopId();
        if (shopId == null || shopId.isEmpty()) {
            Toast.makeText(getContext(), "No shop location saved yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        shopRef = FirebaseDatabase.getInstance().getReference("shops").child(shopId);
        shopRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lng = snapshot.child("lng").getValue(Double.class);

                if (lat == null || lng == null) {
                    Toast.makeText(getContext(), "Shop location not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show dialog with OSMDroid MapView
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                View mapViewDialog = getLayoutInflater().inflate(R.layout.dialog_map, null);
                MapView map = mapViewDialog.findViewById(R.id.dialogMapView);
                map.setTileSource(TileSourceFactory.MAPNIK);
                map.setMultiTouchControls(true);
                map.getController().setZoom(16.0);
                GeoPoint point = new GeoPoint(lat, lng);

                Marker marker = new Marker(map);
                marker.setPosition(point);
                marker.setTitle("Your Shop");
                map.getOverlays().add(marker);
                map.getController().setCenter(point);

                builder.setView(mapViewDialog)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
