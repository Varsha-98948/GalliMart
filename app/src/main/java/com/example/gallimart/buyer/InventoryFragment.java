package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private static final String ARG_SHOP_ID = "shopId";

    private String shopId;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private final List<Item> itemList = new ArrayList<>();
    private DatabaseReference firebaseRef;

    private ImageButton btnViewCart; // Floating cart button

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
        if (getArguments() != null) {
            shopId = getArguments().getString(ARG_SHOP_ID);
        }

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

        adapter = new InventoryAdapter(itemList, this);
        recyclerView.setAdapter(adapter);

        btnViewCart = view.findViewById(R.id.btnViewCart);
        btnViewCart.setOnClickListener(v -> openCartFragment());

        if (shopId != null) {
            setupFirebaseAndFetch(shopId);
        } else {
            fetchShopIdFromUser();
        }

        return view;
    }

    private void fetchShopIdFromUser() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            Toast.makeText(getContext(), "User not signed in and no shopId provided", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("shopId");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shopId = snapshot.getValue(String.class);
                if (shopId != null) {
                    setupFirebaseAndFetch(shopId);
                } else {
                    Toast.makeText(getContext(), "No shopId found for this user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFirebaseAndFetch(String shopId) {
        firebaseRef = FirebaseDatabase.getInstance()
                .getReference("shops")
                .child(shopId)
                .child("items");
        fetchItemsFromFirebase();
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

    public void openCartFragment() {
        CartFragment cartFragment = new CartFragment();
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, cartFragment)
                .addToBackStack(null)
                .commit();
    }

    public void showItemAddedPopup(String itemName) {
        Toast.makeText(getContext(), itemName + " added to cart", Toast.LENGTH_SHORT).show();
    }

    public InventoryAdapter getAdapter() {
        return adapter;
    }
}
