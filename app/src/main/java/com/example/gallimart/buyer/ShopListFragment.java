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
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.*;

public class ShopListFragment extends Fragment implements ShopAdapter.OnShopClickListener {

    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private List<Shop> shopList = new ArrayList<>();
    private DatabaseReference firebaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewShops);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShopAdapter(shopList, this);
        recyclerView.setAdapter(adapter);

        firebaseRef = FirebaseDatabase.getInstance().getReference("shops");
        fetchShopsFromFirebase();

        return view;
    }

    private void fetchShopsFromFirebase() {
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shopList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Shop shop = ds.getValue(Shop.class);
                    if (shop != null) shopList.add(shop);
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

    @Override
    public void onShopClick(Shop shop) {
        InventoryFragment fragment = InventoryFragment.newInstance(shop.getId());
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
