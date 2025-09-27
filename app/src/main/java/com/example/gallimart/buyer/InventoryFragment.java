package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private String shopId;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private final List<Item> itemList = new ArrayList<>();
    private SessionManager session;
    private ImageButton fabCart;

    // keep a reference to our listener
    private final SessionManager.CartChangeListener cartChangeListener = () -> {
        if (adapter != null) {
            adapter.refreshFromCart(); // refresh quantities
            updateCartBadge();
        }
    };

    // STATIC FACTORY METHOD
    public static InventoryFragment newInstance(String shopId) {
        InventoryFragment fragment = new InventoryFragment();
        Bundle args = new Bundle();
        args.putString("shopId", shopId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());

        if (getArguments() != null) shopId = getArguments().getString("shopId");
        if (shopId == null) shopId = session.getShopId();
        if (shopId == null) throw new IllegalStateException("Shop ID not found!");

        // ✅ register our listener
        session.addCartChangeListener(cartChangeListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_buyer_inventory, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewInventory);
        fabCart = view.findViewById(R.id.fabCart);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(itemList, session, () -> {
            if (getActivity() instanceof BuyerDashboardActivity) {
                ((BuyerDashboardActivity) getActivity()).switchToCartTab();
            }
        });
        recyclerView.setAdapter(adapter);

        fabCart.setOnClickListener(v -> {
            if (getActivity() instanceof BuyerDashboardActivity) {
                ((BuyerDashboardActivity) getActivity()).switchToCartTab();
            }
        });

        loadInventoryFromFirebase();
        updateCartBadge();

        return view;
    }

    private void loadInventoryFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("shops")
                .child(shopId)
                .child("items");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Item item = ds.getValue(Item.class);
                    if (item != null) itemList.add(item);
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void updateCartBadge() {
        int count = session.getCartItemCount();
        // update your badge here (placeholder icon for now)
        fabCart.setImageResource(count > 0 ? R.drawable.ic_inventory : R.drawable.ic_inventory);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.refreshFromCart();
        updateCartBadge();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ✅ unregister listener to prevent leaks
        session.removeCartChangeListener(cartChangeListener);
    }
}
