package com.example.gallimart.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.driver.DriverLocationAdapter;
import com.example.gallimart.Order;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment {

    private static final String TAG = "LocationFragment";

    private RecyclerView rvDriverLocations;
    private DriverLocationAdapter adapter;
    private List<DriverLocation> locationList;

    private MapView mapView;
    private Marker driverMarker;
    private Marker shopMarker;
    private Marker buyerMarker;
    private Polyline roadOverlay;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private DatabaseReference userRef, ordersRef;
    private String driverId;
    private String assignedOrderId;

    private Button btnAcceptOrder, btnRejectOrder;

    // keep shop/buyer coordinates so we can compute distances to driver
    private Double currentShopLat = null, currentShopLng = null;
    private Double currentBuyerLat = null, currentBuyerLng = null;

    private final DecimalFormat df = new DecimalFormat("#.##");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_driver, container, false);

        // OSMDroid config
        Configuration.getInstance().load(getContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);

        rvDriverLocations = view.findViewById(R.id.rvDriverLocations);
        rvDriverLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        locationList = new ArrayList<>();
        adapter = new DriverLocationAdapter(locationList);
        rvDriverLocations.setAdapter(adapter);

        btnAcceptOrder = view.findViewById(R.id.btnAcceptOrderDriver);
        btnRejectOrder = view.findViewById(R.id.btnRejectOrderDriver);

        // Firebase references
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Driver not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }
        driverId = user.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(driverId);
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        startLocationUpdates();

        listenForAssignedOrder();

        btnAcceptOrder.setOnClickListener(v -> acceptAssignedOrder());
        btnRejectOrder.setOnClickListener(v -> rejectAssignedOrder());

        return view;
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(5000);
        request.setFastestInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    updateDriverLocation(loc.getLatitude(), loc.getLongitude());
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }

    private void updateDriverLocation(double lat, double lng) {
        // update driver's own user node
        userRef.child("address/lat").setValue(lat);
        userRef.child("address/lng").setValue(lng);

        // also publish to orders/{orderId}/driverLocation so buyer can listen
        if (assignedOrderId != null) {
            DatabaseReference orderDriverLocRef = ordersRef.child(assignedOrderId).child("driverLocation");
            orderDriverLocRef.child("lat").setValue(lat);
            orderDriverLocRef.child("lng").setValue(lng);
        }

        // update marker on the map
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            driverMarker.setTitle("You (Driver)");
            mapView.getOverlays().add(driverMarker);
        }

        // show distance to buyer if we have buyer coords
        if (currentBuyerLat != null && currentBuyerLng != null) {
            double remainingKm = calculateDistanceKm(lat, lng, currentBuyerLat, currentBuyerLng);
            driverMarker.setTitle("You (Driver) - " + df.format(remainingKm) + " km to delivery");
        } else {
            driverMarker.setTitle("You (Driver)");
        }

        driverMarker.setPosition(new GeoPoint(lat, lng));

        // ensure driver marker is on top
        mapView.getOverlays().remove(driverMarker);
        mapView.getOverlays().add(driverMarker);

        mapView.getController().setCenter(new GeoPoint(lat, lng));
        mapView.invalidate();
    }

    private void listenForAssignedOrder() {
        userRef.child("currentOrderId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedOrderId = snapshot.getValue(String.class);
                locationList.clear();

                if (assignedOrderId != null && !assignedOrderId.isEmpty()) {
                    btnAcceptOrder.setEnabled(true);
                    btnRejectOrder.setEnabled(true);

                    ordersRef.child(assignedOrderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot orderSnap) {
                            Order order = orderSnap.getValue(Order.class);
                            if (order != null) {
                                String shopId = order.shopId;
                                String buyerId = order.buyerId;

                                DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference("shops").child(shopId);
                                DatabaseReference buyerRef = FirebaseDatabase.getInstance().getReference("users").child(buyerId);

                                // 1. Get Shop Coords
                                shopRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot shopSnap) {
                                        currentShopLat = shopSnap.child("address/lat").getValue(Double.class);
                                        currentShopLng = shopSnap.child("address/lng").getValue(Double.class);

                                        String shopName = shopSnap.child("name").getValue(String.class);

                                        // 2. Get Buyer Coords (after shop is known)
                                        buyerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot buyerSnap) {
                                                currentBuyerLat = buyerSnap.child("address/lat").getValue(Double.class);
                                                currentBuyerLng = buyerSnap.child("address/lng").getValue(Double.class);
                                                String buyerName = buyerSnap.child("name").getValue(String.class);

                                                // Now you have both shop + buyer coordinates
                                                reverseGeocode(
                                                        currentShopLat != null ? currentShopLat : 0.0,
                                                        currentShopLng != null ? currentShopLng : 0.0,
                                                        shopLabel -> {
                                                            reverseGeocode(
                                                                    currentBuyerLat != null ? currentBuyerLat : 0.0,
                                                                    currentBuyerLng != null ? currentBuyerLng : 0.0,
                                                                    buyerLabel -> {
                                                                        double distKm = (currentShopLat != null && currentShopLng != null
                                                                                && currentBuyerLat != null && currentBuyerLng != null)
                                                                                ? calculateDistanceKm(currentShopLat, currentShopLng, currentBuyerLat, currentBuyerLng)
                                                                                : 0.0;

                                                                        locationList.clear();
                                                                        String routeSummary = shopLabel + " → " + buyerLabel + " (" + df.format(distKm) + " km)";
                                                                        locationList.add(new DriverLocation("Route", routeSummary));

                                                                        if (order.items != null) {
                                                                            for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                                                                                locationList.add(new DriverLocation(
                                                                                        "Pickup: " + item.name,
                                                                                        "Deliver to buyer: " + (order.buyerName != null ? order.buyerName : buyerName)
                                                                                ));
                                                                            }
                                                                        }

                                                                        adapter.notifyDataSetChanged();

                                                                        if (currentShopLat != null && currentShopLng != null
                                                                                && currentBuyerLat != null && currentBuyerLng != null) {
                                                                            showRouteOnMap(currentShopLat, currentShopLng, currentBuyerLat, currentBuyerLng);
                                                                        }
                                                                    });
                                                        });
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.w(TAG, "buyerRef cancelled: " + error);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.w(TAG, "shopRef cancelled: " + error);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "ordersRef cancelled: " + error);
                        }
                    });
                } else {
                    adapter.notifyDataSetChanged();
                    btnAcceptOrder.setEnabled(false);
                    btnRejectOrder.setEnabled(false);

                    // clear map markers & route if no assignment
                    if (shopMarker != null) mapView.getOverlays().remove(shopMarker);
                    if (buyerMarker != null) mapView.getOverlays().remove(buyerMarker);
                    if (roadOverlay != null) mapView.getOverlays().remove(roadOverlay);
                    mapView.invalidate();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "currentOrderId listener cancelled: " + error);
            }
        });
    }

    private void acceptAssignedOrder() {
        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverStatus").setValue("ACCEPTED");
            // set driver on order so buyer/shop can see which driver accepted
            ordersRef.child(assignedOrderId).child("driverId").setValue(driverId);
            userRef.child("available").setValue(false);

            // optionally set initial driverLocation on accept if we have a last known marker
            if (driverMarker != null && driverMarker.getPosition() != null) {
                double lat = driverMarker.getPosition().getLatitude();
                double lng = driverMarker.getPosition().getLongitude();
                ordersRef.child(assignedOrderId).child("driverLocation/lat").setValue(lat);
                ordersRef.child(assignedOrderId).child("driverLocation/lng").setValue(lng);
            }
            Toast.makeText(getContext(), "Order Accepted", Toast.LENGTH_SHORT).show();
        }
    }

    private void rejectAssignedOrder() {
        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverStatus").setValue("REJECTED");
            userRef.child("currentOrderId").removeValue();
            userRef.child("available").setValue(true);
            Toast.makeText(getContext(), "Order Rejected", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRouteOnMap(double startLat, double startLng, double endLat, double endLng) {
        GeoPoint startPoint = new GeoPoint(startLat, startLng);
        GeoPoint endPoint = new GeoPoint(endLat, endLng);

        // remove old markers/route if present
        if (shopMarker != null) mapView.getOverlays().remove(shopMarker);
        if (buyerMarker != null) mapView.getOverlays().remove(buyerMarker);
        if (roadOverlay != null) mapView.getOverlays().remove(roadOverlay);

        // add new shop marker
        shopMarker = new Marker(mapView);
        shopMarker.setPosition(startPoint);
        shopMarker.setTitle("Shop");
        mapView.getOverlays().add(shopMarker);

        // add new buyer marker
        buyerMarker = new Marker(mapView);
        buyerMarker.setPosition(endPoint);
        buyerMarker.setTitle("Buyer");
        mapView.getOverlays().add(buyerMarker);

        // ensure driver marker present (if exists)
        if (driverMarker != null) {
            mapView.getOverlays().remove(driverMarker);
            mapView.getOverlays().add(driverMarker);
        }

        // Request route (network call) on background thread
        new Thread(() -> {
            try {
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(startPoint);
                waypoints.add(endPoint);

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

                        // center map between start and end (simple midpoint)
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

    private interface GeocodeCallback {
        void onResult(String name);
    }

    private void reverseGeocode(double lat, double lng, GeocodeCallback callback) {
        // run in background; Geocoder may use network
        new Thread(() -> {
            String result = "Unknown";
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address a = addresses.get(0);
                    if (a.getLocality() != null) result = a.getLocality();
                    else if (a.getSubLocality() != null) result = a.getSubLocality();
                    else if (a.getFeatureName() != null) result = a.getFeatureName();
                    else if (a.getThoroughfare() != null) result = a.getThoroughfare();
                    else result = a.getAddressLine(0);
                }
            } catch (IOException e) {
                Log.w(TAG, "Geocoder failed: " + e.getMessage());
            }
            final String finalResult = result;
            requireActivity().runOnUiThread(() -> callback.onResult(finalResult));
        }).start();
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
