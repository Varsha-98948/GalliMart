package com.example.gallimart.shopkeeper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.InventoryItem;
import com.example.gallimart.ItemAdapter;
import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the logged-in shopkeeper’s inventory and lets them add new items.
 */
public class InventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<InventoryItem> itemList = new ArrayList<>();
    private DatabaseReference firebaseRef;
    private String shopId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ItemAdapter(getContext(), itemList);
        recyclerView.setAdapter(adapter);

        // get shopId from arguments or SessionManager
        if (getArguments() != null) {
            shopId = getArguments().getString("shopId");
        }
        if (shopId == null) {
            SessionManager sm = new SessionManager(requireContext());
            shopId = sm.getShopId();
        }

        if (shopId == null) {
            Toast.makeText(getContext(), "Shop ID not found!", Toast.LENGTH_SHORT).show();
        } else {
            // point to /shops/{shopId}/items
            firebaseRef = FirebaseDatabase.getInstance()
                    .getReference("shops")
                    .child(shopId)
                    .child("items");
            fetchItemsFromFirebase();
        }

        // FloatingActionButton to add new item
        FloatingActionButton fabAddItem = view.findViewById(R.id.btnAddItem);
        fabAddItem.setOnClickListener(v -> {
            // open AddItemFragment when clicked
            AddItemFragment addItemFragment = new AddItemFragment();

            // pass shopId to AddItemFragment if needed
            Bundle args = new Bundle();
            args.putString("shopId", shopId);
            addItemFragment.setArguments(args);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, addItemFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void fetchItemsFromFirebase() {
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    InventoryItem item = ds.getValue(InventoryItem.class);
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
