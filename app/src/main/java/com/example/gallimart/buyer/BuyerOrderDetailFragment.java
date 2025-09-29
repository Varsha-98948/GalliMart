package com.example.gallimart.buyer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;

import java.util.ArrayList;

public class BuyerOrderDetailFragment extends Fragment {

    private static final String TAG = "BuyerOrderDetailFragment";

    private MapView mapView;
    private Marker shopMarker, buyerMarker, driverMarker;
    private Polyline roadOverlay;

    private DatabaseReference orderRef;
    private String orderId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_detail, container, false);

        Configuration.getInstance().load(requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));

        mapView = view.findViewById(R.id.mapViewDetail);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);

        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
        }
        if (orderId == null) return view;

        orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        listenOrderUpdates();

        return view;
    }

    private void listenOrderUpdates() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double shopLat = snapshot.child("shopLocation/lat").getValue(Double.class);
                Double shopLng = snapshot.child("shopLocation/lng").getValue(Double.class);
                Double buyerLat = snapshot.child("buyerLocation/lat").getValue(Double.class);
                Double buyerLng = snapshot.child("buyerLocation/lng").getValue(Double.class);
                Double driverLat = snapshot.child("driverLocation/lat").getValue(Double.class);
                Double driverLng = snapshot.child("driverLocation/lng").getValue(Double.class);

                mapView.getOverlays().clear();

                // Shop marker
                if (shopLat != null && shopLng != null) {
                    shopMarker = new Marker(mapView);
                    shopMarker.setPosition(new GeoPoint(shopLat, shopLng));
                    shopMarker.setTitle("Shop");
                    mapView.getOverlays().add(shopMarker);
                }

                // Buyer marker
                if (buyerLat != null && buyerLng != null) {
                    buyerMarker = new Marker(mapView);
                    buyerMarker.setPosition(new GeoPoint(buyerLat, buyerLng));
                    buyerMarker.setTitle("You (Buyer)");
                    mapView.getOverlays().add(buyerMarker);
                }

                // Driver marker
                if (driverLat != null && driverLng != null) {
                    driverMarker = new Marker(mapView);
                    driverMarker.setPosition(new GeoPoint(driverLat, driverLng));
                    driverMarker.setTitle("Driver");
                    mapView.getOverlays().add(driverMarker);
                }

                // Draw route (shop → buyer)
                if (shopLat != null && shopLng != null && buyerLat != null && buyerLng != null) {
                    drawRoute(shopLat, shopLng, buyerLat, buyerLng);
                }

                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "listenOrderUpdates cancelled: " + error);
            }
        });
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

                        // Center map
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
