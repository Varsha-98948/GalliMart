package com.example.gallimart.driver;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.Order;
import com.example.gallimart.R;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LocationFragment extends Fragment {

    private static final String TAG = "LocationFragment";
    private static final int CAMERA_REQUEST_CODE = 103;

    private RecyclerView rvDriverLocations;
    private DriverLocationAdapter adapter;
    private ArrayList<DriverLocation> locationList;

    private MapView mapView;
    private Marker driverMarker, shopMarker, buyerMarker;
    private Polyline roadOverlay;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private DatabaseReference userRef, ordersRef, shopsRef, usersRef;
    private String driverId;
    private String assignedOrderId;
    private String lastWatchedOrderId = null;
    private ValueEventListener currentOrderListener = null;

    private Double currentShopLat = null, currentShopLng = null;
    private Double currentBuyerLat = null, currentBuyerLng = null;

    private final DecimalFormat df = new DecimalFormat("#.##");

    // Photo handling
    private String pendingPhotoOrderId = null;
    private Uri deliveryPhotoUri = null;

    public LocationFragment() { /* empty */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_driver, container, false);

        Configuration.getInstance().load(requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        // More zoomed-in default
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(18.6426378, 73.7560766)); // safe default

        rvDriverLocations = view.findViewById(R.id.rvDriverLocations);
        rvDriverLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        locationList = new ArrayList<>();
        adapter = new DriverLocationAdapter(locationList, new DriverLocationAdapter.OnItemActionListener() {
            @Override
            public void onAccept(String orderId) {
                acceptAssignedOrder(orderId);
            }

            @Override
            public void onReject(String orderId) {
                rejectAssignedOrder(orderId);
            }

            @Override
            public void onUploadPhoto(String orderId) {
                captureDeliveryPhoto(orderId);
            }

            @Override
            public void onMarkDelivered(String orderId) {
                markOrderDelivered(orderId);
            }

            @Override
            public void onDirections(String orderId, Double lat, Double lng) {
                openDirectionsToShopThenBuyer();
            }


            @Override
            public void onShowRoute(String orderId, Double shopLat, Double shopLng, Double buyerLat, Double buyerLng) {
                // show route on map for that order
                showRouteOnMap(shopLat, shopLng, buyerLat, buyerLng);
            }
        });
        rvDriverLocations.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Driver not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        driverId = user.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(driverId);
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        shopsRef = FirebaseDatabase.getInstance().getReference("shops");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        startLocationUpdates();

        listenForAvailableOrders();
        listenForAssignedOrder();

        return view;
    }

    /** --- LOCATION UPDATES --- **/
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(5000);
        request.setFastestInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                android.location.Location loc = locationResult.getLastLocation();
                if (loc != null) updateDriverLocation(loc.getLatitude(), loc.getLongitude());
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
        if (mapView == null) return;

        userRef.child("address/lat").setValue(lat);
        userRef.child("address/lng").setValue(lng);

        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverLocation/lat").setValue(lat);
            ordersRef.child(assignedOrderId).child("driverLocation/lng").setValue(lng);
        }

        checkProximityToShopAndBuyer(lat,lng);

        requireActivity().runOnUiThread(() -> {
            if (driverMarker == null) {
                driverMarker = new Marker(mapView);
                driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(driverMarker);
            }

            driverMarker.setPosition(new GeoPoint(lat, lng));
            double remainingKm = 0;
            if (currentBuyerLat != null && currentBuyerLng != null) {
                remainingKm = calculateDistanceKm(lat, lng, currentBuyerLat, currentBuyerLng);
            }
            driverMarker.setTitle("You (Driver)" + (remainingKm > 0 ? " - " + df.format(remainingKm) + " km to delivery" : ""));

            if (!mapView.getOverlays().contains(driverMarker)) {
                mapView.getOverlays().add(driverMarker);
            }
            // keep driver reasonably centered
            mapView.getController().setCenter(new GeoPoint(lat, lng));
            mapView.invalidate();
        });
    }

    // new helper: clear local order state & UI
    private void clearLocalOrderState() {
        assignedOrderId = null;
        lastWatchedOrderId = null;
        if (currentOrderListener != null && lastWatchedOrderId != null) {
            ordersRef.child(lastWatchedOrderId).removeEventListener(currentOrderListener);
            currentOrderListener = null;
        }
        requireActivity().runOnUiThread(() -> {
            locationList.clear();
            adapter.clearOrderContext();
            adapter.notifyDataSetChanged();
            if (shopMarker != null) { mapView.getOverlays().remove(shopMarker); shopMarker = null; }
            if (buyerMarker != null) { mapView.getOverlays().remove(buyerMarker); buyerMarker = null; }
            if (roadOverlay != null) { mapView.getOverlays().remove(roadOverlay); roadOverlay = null; }
            mapView.invalidate();
        });
    }

    // Listen for available (unassigned/new) orders so that new ones show automatically
    private void listenForAvailableOrders() {
        ordersRef.orderByChild("status").equalTo("CONFIRMED")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (assignedOrderId != null && !assignedOrderId.isEmpty()) return;

                        locationList.clear();

                        userRef.child("address").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot driverAddrSnap) {
                                Double driverLat = driverAddrSnap.child("lat").getValue(Double.class);
                                Double driverLng = driverAddrSnap.child("lng").getValue(Double.class);
                                if (driverLat == null || driverLng == null) return;

                                for (DataSnapshot orderSnap : snapshot.getChildren()) {
                                    String orderId = orderSnap.getKey();
                                    if (orderId == null || orderId.trim().isEmpty()) continue; // ✅ skip null order IDs
                                    String drvStatus = orderSnap.child("driverStatus").getValue(String.class);
                                    if (drvStatus != null && !drvStatus.isEmpty()) continue;

                                    String shopId = orderSnap.child("shopId").getValue(String.class);
                                    String buyerId = orderSnap.child("buyerId").getValue(String.class);
                                    if (shopId == null || buyerId == null) continue;

                                    shopsRef.child(shopId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot shopSnap) {
                                            Double shopLat = shopSnap.child("lat").getValue(Double.class);
                                            Double shopLng = shopSnap.child("lng").getValue(Double.class);
                                            if (shopLat == null || shopLng == null) return;

                                            // Fetch buyer coordinates
                                            FirebaseDatabase.getInstance().getReference("users")
                                                    .child(buyerId).child("address")
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot buyerAddrSnap) {
                                                            Double buyerLat = buyerAddrSnap.child("lat").getValue(Double.class);
                                                            Double buyerLng = buyerAddrSnap.child("lng").getValue(Double.class);

                                                            double distKm = calculateDistanceKm(driverLat, driverLng, shopLat, shopLng);

                                                            if (distKm <= 7.0) { // within 7 km only
                                                                Order order = orderSnap.getValue(Order.class);
                                                                if (order != null && order.items != null) {
                                                                    for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                                                                        locationList.add(new DriverLocation(
                                                                                "Deliver: " + item.name + " (" + distKm + " km away)",
                                                                                "To: " + order.buyerName
                                                                        ));
                                                                    }
                                                                }

                                                                adapter.setOrderContext(
                                                                        orderSnap.getKey(),
                                                                        "",
                                                                        shopLat,
                                                                        shopLng,
                                                                        buyerLat,
                                                                        buyerLng,
                                                                        distKm
                                                                );
                                                                adapter.notifyDataSetChanged();

                                                                Log.d(TAG, "Buyer coordinates fetched: " + buyerLat + ", " + buyerLng);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Log.e(TAG, "Buyer address fetch cancelled: " + error.getMessage());
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "Shop fetch cancelled: " + error.getMessage());
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Driver address fetch cancelled: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Available orders listener cancelled: " + error.getMessage());
                    }
                });
    }

    private void markAtShop(String orderId) {
        if (orderId == null) return;
        ordersRef.child(orderId).child("driverStatus").setValue("AT_SHOP");
    }

    private void markAtBuyer(String orderId) {
        if (orderId == null) return;
        ordersRef.child(orderId).child("driverStatus").setValue("AT_BUYER");
    }

    private void checkProximityToShopAndBuyer(double lat, double lng) {
        if (currentShopLat != null && currentShopLng != null) {
            double distToShop = calculateDistanceKm(lat, lng, currentShopLat, currentShopLng);
            if (distToShop <= 0.05) { // ~50 meters
                markAtShop(assignedOrderId);
            }
        }

        if (currentBuyerLat != null && currentBuyerLng != null) {
            double distToBuyer = calculateDistanceKm(lat, lng, currentBuyerLat, currentBuyerLng);
            if (distToBuyer <= 0.05) { // ~50 meters
                markAtBuyer(assignedOrderId);
            }
        }
    }





    /** --- ASSIGNED ORDER HANDLING --- **/
    private void listenForAssignedOrder() {
        userRef.child("currentOrderId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String newOrderId = snapshot.getValue(String.class);

                // Remove old order listener if order assignment changed
                if (lastWatchedOrderId != null && !lastWatchedOrderId.equals(newOrderId) && currentOrderListener != null) {
                    ordersRef.child(lastWatchedOrderId).removeEventListener(currentOrderListener);
                    currentOrderListener = null;
                }

                lastWatchedOrderId = newOrderId;
                assignedOrderId = newOrderId;

                if (assignedOrderId != null && !assignedOrderId.isEmpty()) {

                    currentOrderListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot orderSnap) {
                            if (assignedOrderId == null || assignedOrderId.trim().isEmpty()) {
                                clearLocalOrderState();
                                return;
                            }

                            if (!orderSnap.exists()) {
                                locationList.clear();
                                adapter.notifyDataSetChanged();
                                return;
                            }

                            String orderStatus = orderSnap.child("status").getValue(String.class);
                            String driverStatus = orderSnap.child("driverStatus").getValue(String.class);

                            // If order is delivered, don't show it here
                            if ("DELIVERED".equalsIgnoreCase(orderStatus) || "DELIVERED".equalsIgnoreCase(driverStatus)) {
                                // clear UI for assigned order
                                locationList.clear();
                                adapter.clearOrderContext();
                                adapter.notifyDataSetChanged();
                                return;
                            }

                            // Build list of items (one row per cart item as before)
                            locationList.clear();
                            Order order = orderSnap.getValue(Order.class);
                            if (order != null && order.items != null) {
                                for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                                    locationList.add(new DriverLocation("Deliver: " + item.name, "To: " + (order.buyerName != null && !order.buyerName.isEmpty() ? order.buyerName : "Buyer")));
                                }
                            }
                            adapter.notifyDataSetChanged();
                            Log.d("Data of driverStatus", "onBindViewHolder: " + driverStatus);


                            // load route (shop + buyer) and inform adapter of order context once lat/lng available
                            if (order != null) setupOrderRoute(order, driverStatus);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Order listener cancelled: " + error.getMessage());
                        }
                    };

                    ordersRef.child(assignedOrderId).addValueEventListener(currentOrderListener);

                } else {
                    // No assigned order
                    assignedOrderId = null;
                    locationList.clear();
                    adapter.clearOrderContext();
                    adapter.notifyDataSetChanged();

                    // remove markers/route
                    requireActivity().runOnUiThread(() -> {
                        if (shopMarker != null) { mapView.getOverlays().remove(shopMarker); shopMarker = null; }
                        if (buyerMarker != null) { mapView.getOverlays().remove(buyerMarker); buyerMarker = null; }
                        if (roadOverlay != null) { mapView.getOverlays().remove(roadOverlay); roadOverlay = null; }
                        mapView.invalidate();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen for assigned order: " + error.getMessage());
            }
        });
    }

    // Setup order route now computes distance and passes it to the adapter
    private void setupOrderRoute(Order order, String driverStatus) {
        if (order == null) {
            Log.e(TAG, "setupOrderRoute: order is null");
            return;
        }

        // Check for null IDs to avoid crash
        if (order.shopId == null || order.shopId.trim().isEmpty()) {
            Log.e(TAG, "setupOrderRoute: shopId is null or empty for order " + assignedOrderId);
            return;
        }
        if (order.buyerId == null || order.buyerId.trim().isEmpty()) {
            Log.e(TAG, "setupOrderRoute: buyerId is null or empty for order " + assignedOrderId);
            return;
        }

        DatabaseReference shopRef = shopsRef.child(order.shopId);
        DatabaseReference buyerRef = usersRef.child(order.buyerId);

        shopRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot shopSnap) {
                currentShopLat = shopSnap.child("lat").getValue(Double.class);
                currentShopLng = shopSnap.child("lng").getValue(Double.class);

                buyerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot buyerSnap) {
                        currentBuyerLat = buyerSnap.child("address/lat").getValue(Double.class);
                        currentBuyerLng = buyerSnap.child("address/lng").getValue(Double.class);

                        double routeDistanceKm = 0.0;
                        if (currentShopLat != null && currentShopLng != null && currentBuyerLat != null && currentBuyerLng != null) {
                            routeDistanceKm = calculateDistanceKm(currentShopLat, currentShopLng, currentBuyerLat, currentBuyerLng);
                        }

                        adapter.setOrderContext(
                                assignedOrderId,
                                driverStatus,
                                currentShopLat, currentShopLng,
                                currentBuyerLat, currentBuyerLng,
                                routeDistanceKm
                        );

                        if (currentShopLat != null && currentShopLng != null &&
                                currentBuyerLat != null && currentBuyerLng != null) {
                            showRouteOnMap(currentShopLat, currentShopLng, currentBuyerLat, currentBuyerLng);
                        } else {
                            Log.w(TAG, "Incomplete coordinates, route not drawn");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Buyer fetch cancelled: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Shop fetch cancelled: " + error.getMessage());
            }
        });
    }



    /** --- ACCEPT / REJECT ORDER (accept/reject take orderId param) --- **/
    // ACCEPT with transaction to avoid double-accept races
    private void acceptAssignedOrder(String orderId) {
        if (orderId == null) return;

        DatabaseReference orderRef = ordersRef.child(orderId);
        orderRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    // order no longer exists
                    return Transaction.success(currentData);
                }

                String existingDriverStatus = currentData.child("driverStatus").getValue(String.class);
                String existingDriverId = currentData.child("driverId").getValue(String.class);

                // If no driver has accepted yet -> claim it
                if (existingDriverStatus == null || existingDriverStatus.isEmpty() || "REJECTED".equalsIgnoreCase(existingDriverStatus)) {
                    currentData.child("driverStatus").setValue("ACCEPTED");
                    currentData.child("driverId").setValue(driverId);
                    return Transaction.success(currentData);
                } else {
                    // Someone already accepted — abort
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Accept transaction error: " + error.getMessage());
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Accept failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                    return;
                }

                if (!committed) { // someone else already took it
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Order already accepted by another driver", Toast.LENGTH_SHORT).show();
                            // clear our local context to avoid stale UI
                            clearLocalOrderState();
                            userRef.child("currentOrderId").removeValue();
                            userRef.child("available").setValue(true);
                        });
                    return;
                }

                // successful claim: update driver node and UI
                userRef.child("currentOrderId").setValue(orderId)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                userRef.child("available").setValue(false);
                                ordersRef.child(orderId).child("driverStatus").setValue("ACCEPTED");
                                ordersRef.child(orderId).child("driverId").setValue(driverId);
                                listenForAssignedOrder(); // ✅ refresh assigned order
                                Toast.makeText(getContext(), "Order Accepted", Toast.LENGTH_SHORT).show();
                            }
                        });
                userRef.child("available").setValue(false);
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Order Accepted", Toast.LENGTH_SHORT).show());
            }
        });
    }


    // REJECT: unassign the order so other drivers can accept it
    private void rejectAssignedOrder(String orderId) {
        if (orderId == null) return;

        // Remove driver fields from order so other drivers can accept it.
        ordersRef.child(orderId).child("driverId").removeValue();
        ordersRef.child(orderId).child("driverStatus").removeValue();

        // Clear this driver's assignment
        userRef.child("currentOrderId").removeValue();
        userRef.child("available").setValue(true);

        if (getActivity() != null)
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Order Rejected", Toast.LENGTH_SHORT).show());

        // Clear local UI
        clearLocalOrderState();
    }


    // Improved openGoogleMaps: try google.navigation first, then geo fallback, then chooser
    // ✅ Always open with Google Maps (if installed)
    private void openGoogleMaps(Double lat, Double lng) {
        if (lat == null || lng == null) {
            Toast.makeText(getContext(), "Destination not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Direct Google Maps navigation intent
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // Try to open in Google Maps
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback: open in browser if Maps not installed
                Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, webUri);
                startActivity(browserIntent);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening Google Maps", e);
            Toast.makeText(getContext(), "Unable to open Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    /** --- NEW METHOD: Directions Driver→Shop→Buyer --- **/
    private void openDirectionsToShopThenBuyer() {
        if (currentShopLat == null || currentShopLng == null) {
            Toast.makeText(getContext(), "Shop location not available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentBuyerLat == null || currentBuyerLng == null) {
            Toast.makeText(getContext(), "Buyer location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1️⃣ Open Google Maps for driver → shop
            Uri shopUri = Uri.parse("google.navigation:q=" + currentShopLat + "," + currentShopLng + "&mode=d");
            Intent shopIntent = new Intent(Intent.ACTION_VIEW, shopUri);
            shopIntent.setPackage("com.google.android.apps.maps");

            // Verify Maps app exists
            if (shopIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(shopIntent);

                // 2️⃣ After short delay, open driver → buyer directions
                // (so when driver presses back after reaching shop, next route opens)
                new android.os.Handler().postDelayed(() -> {
                    try {
                        Uri buyerUri = Uri.parse("google.navigation:q=" + currentBuyerLat + "," + currentBuyerLng + "&mode=d");
                        Intent buyerIntent = new Intent(Intent.ACTION_VIEW, buyerUri);
                        buyerIntent.setPackage("com.google.android.apps.maps");
                        startActivity(buyerIntent);
                    } catch (Exception e2) {
                        Log.e(TAG, "Error opening buyer directions", e2);
                        Toast.makeText(getContext(), "Couldn't open buyer route", Toast.LENGTH_SHORT).show();
                    }
                }, 3000); // 3-second delay before auto-opening buyer route (optional)
            } else {
                // fallback: browser
                Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + currentShopLat + "," + currentShopLng);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, webUri);
                startActivity(browserIntent);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening Google Maps route", e);
            Toast.makeText(getContext(), "Unable to open directions", Toast.LENGTH_SHORT).show();
        }
    }


    /** --- PHOTO / CAPTURE --- **/
    private void captureDeliveryPhoto(String orderId) {
        pendingPhotoOrderId = orderId;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(getContext(), "No Camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");

            // Save to cache & get URI
            File photoFile = new File(requireContext().getCacheDir(), "delivery_" + System.currentTimeMillis() + ".jpg");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(photoFile)) {
                imageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                deliveryPhotoUri = Uri.fromFile(photoFile);
                Toast.makeText(getContext(), "Photo captured for order " + pendingPhotoOrderId, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to save photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** --- MARK DELIVERED --- **/
    private void markOrderDelivered(String orderId) {
        if (orderId == null) return;
        if (deliveryPhotoUri == null || pendingPhotoOrderId == null || !pendingPhotoOrderId.equals(orderId)) {
            if (isFragmentActive()) {
                Toast.makeText(getContext(), "Upload a delivery photo first", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new Thread(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(deliveryPhotoUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                String fileName = "delivery_" + System.currentTimeMillis() + ".jpg";
                String supabaseUrl = "https://yxzgowwvyhugzgjiypbk.supabase.co";
                String bucketName = "uploads";
                String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl4emdvd3d2eWh1Z3pnaml5cGJrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc1ODc5NjYsImV4cCI6MjA3MzE2Mzk2Nn0.K694SUdB5djvZvn9QdAr1ZwfgpxBudyDekXCouz4_Y0";
                String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/Delivery/" + fileName;

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
                Request request = new Request.Builder()
                        .url(url)
                        .header("apikey", supabaseKey)
                        .header("Authorization", "Bearer " + supabaseKey)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                        if (isFragmentActive()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;

                            ordersRef.child(orderId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot orderSnap) {
                                    if (!orderSnap.exists() || !isFragmentActive()) return;

                                    DatabaseReference completedRef = FirebaseDatabase.getInstance()
                                            .getReference("completedOrders")
                                            .child(orderId);

                                    completedRef.setValue(orderSnap.getValue()).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            completedRef.child("deliveryPhotoUrl").setValue(publicUrl);
                                            completedRef.child("status").setValue("DELIVERED");
                                            completedRef.child("driverStatus").setValue("DELIVERED");

                                            // Copy to driver's completed orders
                                            userRef.child("completedOrders").child(orderId).setValue(orderSnap.getValue());
                                            userRef.child("completedOrders").child(orderId)
                                                    .child("deliveryPhotoUrl").setValue(publicUrl);

                                            // Remove from /orders/
                                            ordersRef.child(orderId).removeValue();

                                            // Free driver
                                            userRef.child("available").setValue(true);
                                            userRef.child("currentOrderId").removeValue();

                                            if (isFragmentActive()) {
                                                requireActivity().runOnUiThread(() -> {
                                                    Toast.makeText(getContext(),
                                                            "Order delivered & moved to completed orders!",
                                                            Toast.LENGTH_SHORT).show();
                                                    locationList.clear();
                                                    adapter.clearOrderContext();
                                                    adapter.notifyDataSetChanged();
                                                });
                                            }

                                            pendingPhotoOrderId = null;
                                            deliveryPhotoUri = null;
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Failed to move order to completed: " + error.getMessage());
                                }
                            });

                        } else {
                            String body = response.body() != null ? response.body().string() : "unknown";
                            Log.e(TAG, "Supabase upload error: " + body);
                            if (isFragmentActive()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Photo upload failed", Toast.LENGTH_SHORT).show()
                                );
                            }
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (isFragmentActive()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    /** ✅ Helper method to safely check if Fragment is still attached **/
    private boolean isFragmentActive() {
        return isAdded() && getActivity() != null && !isRemoving() && !isDetached();
    }

    /** --- ROUTE & MAP --- **/
    private void showRouteOnMap(double startLat, double startLng, double endLat, double endLng) {
        if (mapView == null || getActivity() == null || !isAdded()) return;

        GeoPoint startPoint = new GeoPoint(startLat, startLng);
        GeoPoint endPoint = new GeoPoint(endLat, endLng);

        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return; // extra safety

            if (shopMarker != null) mapView.getOverlays().remove(shopMarker);
            if (buyerMarker != null) mapView.getOverlays().remove(buyerMarker);
            if (roadOverlay != null) mapView.getOverlays().remove(roadOverlay);

            shopMarker = new Marker(mapView);
            shopMarker.setPosition(startPoint);
            shopMarker.setTitle("Shop");
            mapView.getOverlays().add(shopMarker);

            buyerMarker = new Marker(mapView);
            buyerMarker.setPosition(endPoint);
            buyerMarker.setTitle("Buyer");
            mapView.getOverlays().add(buyerMarker);

            if (driverMarker != null) {
                mapView.getOverlays().remove(driverMarker);
                mapView.getOverlays().add(driverMarker);
            }

            mapView.invalidate();
        });

        // fetch and draw route safely in a background thread
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

                    if (getActivity() == null || !isAdded()) return; // ✅ check again before UI update

                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        roadOverlay = new Polyline(mapView);
                        roadOverlay.setPoints(roadPoints);
                        roadOverlay.setWidth(8f);
                        mapView.getOverlays().add(roadOverlay);

                        double midLat = (startLat + endLat) / 2.0;
                        double midLng = (startLng + endLng) / 2.0;
                        mapView.getController().setCenter(new GeoPoint(midLat, midLng));

                        double distanceKm = calculateDistanceKm(startLat, startLng, endLat, endLng);
                        mapView.getController().setZoom(distanceToZoom(distanceKm));
                        mapView.invalidate();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
            }
        }).start();
    }


    private double distanceToZoom(double km) {
        if (km <= 0.2) return 17.0;
        if (km <= 0.5) return 16.0;
        if (km <= 1.0) return 15.5;
        if (km <= 3.0) return 15.0;
        if (km <= 6.0) return 14.0;
        if (km <= 15.0) return 12.5;
        return 11.0;
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
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
        // remove firebase listeners to avoid leaks
        if (currentOrderListener != null && lastWatchedOrderId != null) {
            ordersRef.child(lastWatchedOrderId).removeEventListener(currentOrderListener);
        }
    }
}


