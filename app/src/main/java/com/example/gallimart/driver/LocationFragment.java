package com.example.gallimart.driver;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LocationFragment extends Fragment {

    private static final String TAG = "LocationFragment";
    private static final int PHOTO_REQUEST_CODE = 102;

    private RecyclerView rvDriverLocations;
    private DriverLocationAdapter adapter;
    private List<DriverLocation> locationList;

    private MapView mapView;
    private Marker driverMarker, shopMarker, buyerMarker;
    private Polyline roadOverlay;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private DatabaseReference userRef, ordersRef;
    private String driverId, assignedOrderId;

    private Button btnAcceptOrder, btnRejectOrder, btnMarkDelivered, btnUploadPhoto;
    private Double currentShopLat = null, currentShopLng = null;
    private Double currentBuyerLat = null, currentBuyerLng = null;

    private final DecimalFormat df = new DecimalFormat("#.##");
    private Uri deliveryPhotoUri = null;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_driver, container, false);

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
        btnMarkDelivered = view.findViewById(R.id.btnMarkDelivered);
        btnUploadPhoto = view.findViewById(R.id.btnUploadPhoto);

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
        btnMarkDelivered.setOnClickListener(v -> markOrderDelivered());
        btnUploadPhoto.setOnClickListener(v -> selectDeliveryPhoto());

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
        if (mapView == null) return; // safety check

        userRef.child("address/lat").setValue(lat);
        userRef.child("address/lng").setValue(lng);

        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverLocation/lat").setValue(lat);
            ordersRef.child(assignedOrderId).child("driverLocation/lng").setValue(lng);
        }

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
            mapView.getController().setCenter(new GeoPoint(lat, lng));
            mapView.invalidate();
        });
    }

    /** --- ASSIGNED ORDER HANDLING --- **/
    private void listenForAssignedOrder() {
        userRef.child("currentOrderId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignedOrderId = snapshot.getValue(String.class);

                if (assignedOrderId != null && !assignedOrderId.isEmpty()) {
                    btnAcceptOrder.setVisibility(View.GONE);
                    btnRejectOrder.setVisibility(View.GONE);
                    btnMarkDelivered.setVisibility(View.VISIBLE);
                    btnUploadPhoto.setVisibility(View.VISIBLE);

                    ordersRef.child(assignedOrderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot orderSnap) {
                            Order order = orderSnap.getValue(Order.class);
                            if (order != null) setupOrderRoute(order);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

                } else {
                    btnAcceptOrder.setVisibility(View.VISIBLE);
                    btnRejectOrder.setVisibility(View.VISIBLE);
                    btnMarkDelivered.setVisibility(View.GONE);
                    btnUploadPhoto.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupOrderRoute(Order order) {
        DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference("shops").child(order.shopId);
        DatabaseReference buyerRef = FirebaseDatabase.getInstance().getReference("users").child(order.buyerId);

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

                        locationList.clear();
                        if (order.items != null) {
                            for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                                locationList.add(new DriverLocation("Deliver: " + item.name, "To: " + buyerSnap.child("name").getValue(String.class)));
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (currentShopLat != null && currentShopLng != null && currentBuyerLat != null && currentBuyerLng != null) {
                            showRouteOnMap(currentShopLat, currentShopLng, currentBuyerLat, currentBuyerLng);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /** --- ACCEPT / REJECT ORDER --- **/
    private void acceptAssignedOrder() {
        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverStatus").setValue("ACCEPTED");
            ordersRef.child(assignedOrderId).child("driverId").setValue(driverId);
            userRef.child("available").setValue(false);
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

    /** --- PHOTO SELECTION --- **/
    private void selectDeliveryPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PHOTO_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            deliveryPhotoUri = data.getData();
            Toast.makeText(getContext(), "Photo selected", Toast.LENGTH_SHORT).show();
        }
    }

    /** --- MARK DELIVERED AND UPLOAD PHOTO TO SUPABASE --- **/
    private void markOrderDelivered() {
        if (assignedOrderId == null) return;
        if (deliveryPhotoUri == null) {
            Toast.makeText(getContext(), "Upload a delivery photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Convert URI to byte array
                InputStream inputStream = requireContext().getContentResolver().openInputStream(deliveryPhotoUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                String fileName = "delivery_" + System.currentTimeMillis() + ".jpg";
                String supabaseUrl = "https://yxzgowwvyhugzgjiypbk.supabase.co"; // your Supabase URL
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
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;

                            // Update Firebase with photo URL and mark order delivered
                            ordersRef.child(assignedOrderId).child("deliveryPhotoUrl").setValue(publicUrl);
                            ordersRef.child(assignedOrderId).child("status").setValue("DELIVERED");
                            userRef.child("available").setValue(true);
                            userRef.child("currentOrderId").removeValue();

                            if (getActivity() != null)
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Order delivered & photo uploaded!", Toast.LENGTH_SHORT).show()
                                );

                        } else {
                            String body = response.body().string();
                            Log.e(TAG, "Supabase upload error: " + body);
                            if (getActivity() != null)
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Photo upload failed", Toast.LENGTH_SHORT).show()
                                );
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
            }
        }).start();
    }

    /** --- ROUTE & MAP --- **/
    private void showRouteOnMap(double startLat, double startLng, double endLat, double endLng) {
        if (mapView == null) return; // safety check

        GeoPoint startPoint = new GeoPoint(startLat, startLng);
        GeoPoint endPoint = new GeoPoint(endLat, endLng);

        requireActivity().runOnUiThread(() -> {
            // Remove old markers/overlays
            if (shopMarker != null) mapView.getOverlays().remove(shopMarker);
            if (buyerMarker != null) mapView.getOverlays().remove(buyerMarker);
            if (roadOverlay != null) mapView.getOverlays().remove(roadOverlay);

            // Add shop marker
            shopMarker = new Marker(mapView);
            shopMarker.setPosition(startPoint);
            shopMarker.setTitle("Shop");
            mapView.getOverlays().add(shopMarker);

            // Add buyer marker
            buyerMarker = new Marker(mapView);
            buyerMarker.setPosition(endPoint);
            buyerMarker.setTitle("Buyer");
            mapView.getOverlays().add(buyerMarker);

            // Re-add driver marker if exists
            if (driverMarker != null) {
                mapView.getOverlays().remove(driverMarker);
                mapView.getOverlays().add(driverMarker);
            }

            mapView.invalidate();
        });

        // Calculate and draw route in background
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
    }
}
