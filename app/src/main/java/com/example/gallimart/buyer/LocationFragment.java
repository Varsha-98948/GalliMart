package com.example.gallimart.buyer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class LocationFragment extends Fragment {

    private RecyclerView rvNearbyShops;
    private ShopAdapter adapter;
    private List<Shop> shopList = new ArrayList<>();

    private DatabaseReference shopsRef;
    private FusedLocationProviderClient fusedLocationClient;
    private Location buyerLocation;

    private static final float MAX_DISTANCE_KM = 7f; // 3 km radius

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_location_buyer, container, false);
        rvNearbyShops = view.findViewById(R.id.rvNearbyShops);
        rvNearbyShops.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShopAdapter(shopList, shop -> {
            // handle click
            Toast.makeText(getContext(), "Clicked: " + shop.getName(), Toast.LENGTH_SHORT).show();
        });
        rvNearbyShops.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        shopsRef = FirebaseDatabase.getInstance().getReference("shops");

        getBuyerLocationAndFetchShops();

        return view;
    }

    private void getBuyerLocationAndFetchShops() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    buyerLocation = location;
                    fetchNearbyShops();
                } else {
                    Toast.makeText(getContext(), "Unable to get your location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchNearbyShops() {
        shopsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shopList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Shop shop = ds.getValue(Shop.class);
                    if(shop != null && shop.getLatitude() != null && shop.getLongitude() != null) {
                        Location shopLoc = new Location("");
                        shopLoc.setLatitude(shop.getLatitude());
                        shopLoc.setLongitude(shop.getLongitude());

                        float distanceKm = buyerLocation.distanceTo(shopLoc)/1000f; // meters → km
                        if(distanceKm <= MAX_DISTANCE_KM){
                            shop.setDistance(distanceKm);
                            shopList.add(shop);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Firebase fetch error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
