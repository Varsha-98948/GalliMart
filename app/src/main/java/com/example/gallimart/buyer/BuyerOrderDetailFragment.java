package com.example.gallimart.buyer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.TextView;

import com.example.gallimart.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class BuyerOrderDetailFragment extends Fragment {

    private static final String TAG = "BuyerOrderDetailFragment";

    private MapView mapView;
    private Marker shopMarker, buyerMarker, driverMarker;
    private Polyline roadOverlay;

    private DatabaseReference orderRef;
    private String orderId;

    // Progress UI elements
    private View dotPlaced, dotConfirmed, dotDriverShop, dotDriverBuyer, dotDelivered;
    private TextView tvOrderStatusLabel;

    // Coordinates
    private Double shopLat, shopLng, buyerLat, buyerLng, driverLat, driverLng;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_order_detail, container, false);

        // Initialize progress UI
        dotPlaced = view.findViewById(R.id.dotPlaced);
        dotConfirmed = view.findViewById(R.id.dotConfirmed);
        dotDriverShop = view.findViewById(R.id.dotDriverShop);
        dotDriverBuyer = view.findViewById(R.id.dotDriverBuyer);
        dotDelivered = view.findViewById(R.id.dotDelivered);
        tvOrderStatusLabel = view.findViewById(R.id.tvOrderStatusLabel);

        // Initialize map
        Configuration.getInstance().load(requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));

        mapView = view.findViewById(R.id.mapViewDetail);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);

        // Get orderId
        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
        }
        if (orderId == null) return view;

        // Firebase ref
        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        // Start listening for live updates
        listenOrderUpdates();

        return view;
    }

    /** --- Live Order Updates --- **/
    private void listenOrderUpdates() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String orderStatus = snapshot.child("status").getValue(String.class);
                String driverStatus = snapshot.child("driverStatus").getValue(String.class);

                shopLat = snapshot.child("shopLocation/lat").getValue(Double.class);
                shopLng = snapshot.child("shopLocation/lng").getValue(Double.class);
                buyerLat = snapshot.child("buyerLocation/lat").getValue(Double.class);
                buyerLng = snapshot.child("buyerLocation/lng").getValue(Double.class);
                driverLat = snapshot.child("driverLocation/lat").getValue(Double.class);
                driverLng = snapshot.child("driverLocation/lng").getValue(Double.class);

                // Update UI
                updateProgressUI(orderStatus, driverStatus);
                updateMapMarkers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "listenOrderUpdates cancelled: " + error);
            }
        });
    }

    /** --- Update Progress Bar --- **/
    private void updateProgressUI(String status, String driverStatus) {
        resetProgressDots();

        if (status == null) return;
        tvOrderStatusLabel.setText("Order Status: " + status);

        // Step 1: PLACED
        if (status.equalsIgnoreCase("PLACED")) {
            activate(dotPlaced);
        }

        // Step 2: CONFIRMED
        if (status.equalsIgnoreCase("CONFIRMED")) {
            activate(dotPlaced);
            activate(dotConfirmed);
        }

        // Step 3: Driver at Shop
        if ("AT_SHOP".equalsIgnoreCase(driverStatus)) {
            activate(dotPlaced);
            activate(dotConfirmed);
            activate(dotDriverShop);
        }

        // Step 4: Driver near Buyer
        if ("AT_BUYER".equalsIgnoreCase(driverStatus)) {
            activate(dotPlaced);
            activate(dotConfirmed);
            activate(dotDriverShop);
            activate(dotDriverBuyer);
        }

        // Step 5: Delivered
        if (status.equalsIgnoreCase("DELIVERED")) {
            activate(dotPlaced);
            activate(dotConfirmed);
            activate(dotDriverShop);
            activate(dotDriverBuyer);
            activate(dotDelivered);
        }

        // --- Proximity-based auto progress (optional real-time feedback) ---
        if (driverLat != null && shopLat != null) {
            double distToShop = calculateDistanceKm(driverLat, driverLng, shopLat, shopLng);
            if (distToShop < 0.1) { // within 100m
                activate(dotDriverShop);
            }
        }

        if (driverLat != null && buyerLat != null) {
            double distToBuyer = calculateDistanceKm(driverLat, driverLng, buyerLat, buyerLng);
            if (distToBuyer < 0.15) { // within 150m
                activate(dotDriverBuyer);
            }
        }
    }

    private void resetProgressDots() {
        dotPlaced.setBackgroundResource(R.drawable.progress_dot_inactive);
        dotConfirmed.setBackgroundResource(R.drawable.progress_dot_inactive);
        dotDriverShop.setBackgroundResource(R.drawable.progress_dot_inactive);
        dotDriverBuyer.setBackgroundResource(R.drawable.progress_dot_inactive);
        dotDelivered.setBackgroundResource(R.drawable.progress_dot_inactive);
    }

    private void activate(View dot) {
        dot.setBackgroundResource(R.drawable.progress_dot_active);
    }

    /** --- Distance Helper --- **/
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of Earth
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** --- Update Map Markers & Route --- **/
    /** Update map markers and focus on driver **/
    private void updateMapMarkers() {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        if (shopLat != null && shopLng != null) {
            shopMarker = new Marker(mapView);
            shopMarker.setPosition(new GeoPoint(shopLat, shopLng));
            shopMarker.setTitle("Shop");
            mapView.getOverlays().add(shopMarker);
        }

        if (buyerLat != null && buyerLng != null) {
            buyerMarker = new Marker(mapView);
            buyerMarker.setPosition(new GeoPoint(buyerLat, buyerLng));
            buyerMarker.setTitle("You (Buyer)");
            mapView.getOverlays().add(buyerMarker);
        }

        if (driverLat != null && driverLng != null) {
            driverMarker = new Marker(mapView);
            driverMarker.setPosition(new GeoPoint(driverLat, driverLng));
            driverMarker.setTitle("Driver");
            mapView.getOverlays().add(driverMarker);

            // Focus map on driver
            mapView.getController().setCenter(new GeoPoint(driverLat, driverLng));
            mapView.getController().setZoom(15.0); // Zoom closer to driver
        }

        mapView.invalidate();
    }


    private void drawRoute(double startLat, double startLng, double endLat, double endLng) {
        new Thread(() -> {
            try {
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(new GeoPoint(startLat, startLng));
                waypoints.add(new GeoPoint(endLat, endLng));

                OSRMRoadManager roadManager = new OSRMRoadManager(requireContext());
                Road road = roadManager.getRoad(waypoints);

                if (road != null && road.mNodes != null && !road.mNodes.isEmpty()) {
                    ArrayList<GeoPoint> roadPoints = new ArrayList<>();
                    for (RoadNode node : road.mNodes) {
                        roadPoints.add(node.mLocation);
                    }

                    Polyline newRoadOverlay = new Polyline(mapView);
                    newRoadOverlay.setPoints(roadPoints);
                    newRoadOverlay.setWidth(8f);

                    requireActivity().runOnUiThread(() -> {
                        roadOverlay = newRoadOverlay;
                        mapView.getOverlays().add(roadOverlay);

                        // Center map between shop and buyer
                        double midLat = (startLat + endLat) / 2.0;
                        double midLng = (startLng + endLng) / 2.0;
                        mapView.getController().setCenter(new GeoPoint(midLat, midLng));
                        mapView.invalidate();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
            }
        }).start();
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
