package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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

    private final SessionManager.CartChangeListener cartChangeListener = () -> {
        if (adapter != null) adapter.refreshFromCart();
        updateCartBadge();
    };

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

        // Grid layout for 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new InventoryAdapter(itemList, session, () -> {
            if (getActivity() instanceof BuyerDashboardActivity) {
                ((BuyerDashboardActivity) getActivity()).switchToCartTab();
            }
        });
        recyclerView.setAdapter(adapter);

        // Apply layout animation
        recyclerView.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_fade_in)
        );
        recyclerView.scheduleLayoutAnimation();

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
                recyclerView.scheduleLayoutAnimation(); // trigger animation on new data
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void updateCartBadge() {
        int count = session.getCartItemCount();
        fabCart.setImageResource(count > 0 ? R.drawable.ic_cart : R.drawable.ic_cart);
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
        session.removeCartChangeListener(cartChangeListener);
    }
}
