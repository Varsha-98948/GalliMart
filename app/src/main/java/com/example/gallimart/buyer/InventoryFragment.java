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
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryFragment extends Fragment {

    private static final String ARG_SHOP_ID = "shopId";

    private String shopId;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private SessionManager session;
    private ImageButton fabCart;

    public static InventoryFragment newInstance(String shopId) {
        InventoryFragment fragment = new InventoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_ID, shopId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());
        if (getArguments() != null) shopId = getArguments().getString(ARG_SHOP_ID);
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

        // Load cart from session
        Map<String, SessionManager.CartItem> savedCart = session.getCart();
        adapter = new InventoryAdapter(itemList, this, session, savedCart);
        recyclerView.setAdapter(adapter);

        // Floating button opens cart
        fabCart.setOnClickListener(v -> navigateToCart());

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
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void navigateToCart() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CartFragment())
                .addToBackStack(null)
                .commit();
    }

    public void updateCartBadge() {
        int count = session.getCartItemCount();
        // You can also show a badge count drawable if you like
        fabCart.setImageResource(count > 0 ? R.drawable.ic_inventory : R.drawable.ic_inventory);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCartBadge();
    }
}
