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
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

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

        if (getArguments() != null) {
            shopId = getArguments().getString("shopId");
        }

        firebaseRef = FirebaseDatabase.getInstance().getReference("items");
        fetchItemsFromFirebase();

        return view;
    }

    private void fetchItemsFromFirebase() {
        firebaseRef.orderByChild("shopId").equalTo(shopId)
                .addValueEventListener(new ValueEventListener() {
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
