package com.example.gallimart.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.example.gallimart.driver.adapters.DriverLocationAdapter;
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

import java.util.ArrayList;
import java.util.List;

public class LocationFragment extends Fragment {

    private RecyclerView rvDriverLocations;
    private DriverLocationAdapter adapter;
    private List<DriverLocation> locationList;

    private MapView mapView;
    private Marker driverMarker;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private DatabaseReference userRef, ordersRef;
    private String driverId;
    private String assignedOrderId;

    private Button btnAcceptOrder, btnRejectOrder;

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
        userRef.child("address/lat").setValue(lat);
        userRef.child("address/lng").setValue(lng);

        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            driverMarker.setTitle("You (Driver)");
            mapView.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(new GeoPoint(lat, lng));
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
                                for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                                    locationList.add(new DriverLocation(
                                            "Pickup: " + item.name,
                                            "Delivery to buyer: " + order.buyerName
                                    ));
                                }
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    adapter.notifyDataSetChanged();
                    btnAcceptOrder.setEnabled(false);
                    btnRejectOrder.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void acceptAssignedOrder() {
        if (assignedOrderId != null) {
            ordersRef.child(assignedOrderId).child("driverStatus").setValue("ACCEPTED");
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
