package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private static final String ARG_SHOP_ID = "shopId";

    private String shopId;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private DatabaseReference firebaseRef;

    // Create a new instance and pass the shopId (optional)
    public static InventoryFragment newInstance(String shopId) {
        InventoryFragment fragment = new InventoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_ID, shopId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Try to get shopId from arguments
        if (getArguments() != null) {
            shopId = getArguments().getString(ARG_SHOP_ID);
        }

        // Fallback: get shopId from SessionManager
        if (shopId == null) {
            SessionManager session = new SessionManager(requireContext());
            shopId = session.getShopId();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_buyer_inventory, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewInventory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(itemList);
        recyclerView.setAdapter(adapter);

        if (shopId == null) {
            Toast.makeText(getContext(), "Shop ID not found!", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Firebase reference: /shops/{shopId}/items
        firebaseRef = FirebaseDatabase.getInstance()
                .getReference("shops")
                .child(shopId)
                .child("items");

        fetchItemsFromFirebase();

        return view;
    }

    private void fetchItemsFromFirebase() {
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Item item = ds.getValue(Item.class);
                    if (item != null) itemList.add(item);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        "Firebase fetch error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
