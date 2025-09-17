package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvTotalPrice;
    private CartAdapter cartAdapter;
    private List<Item> cartItems = new ArrayList<>();
    private Map<String, Integer> cartQuantities;

    public static CartFragment newInstance(Map<String, Integer> cartQuantities) {
        CartFragment fragment = new CartFragment();
        Bundle args = new Bundle();
        args.putSerializable("cartQuantities", new java.util.HashMap<>(cartQuantities));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cartQuantities = (Map<String, Integer>) getArguments().getSerializable("cartQuantities");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCart);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(cartItems, cartQuantities, this::updateTotalPrice);
        recyclerView.setAdapter(cartAdapter);

        loadCartItems();
        updateTotalPrice();

        return view;
    }

    private void loadCartItems() {
        cartItems.clear();
        if (cartQuantities != null) {
            for (Map.Entry<String, Integer> entry : cartQuantities.entrySet()) {
                // For simplicity, here we create a dummy Item with the itemId
                // In real case, you should fetch actual item details from Firebase
                Item item = new Item(entry.getKey(), "Item Name", 100, null);
                cartItems.add(item);
            }
        }
        cartAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        double total = 0;
        for (Item item : cartItems) {
            int qty = cartQuantities.getOrDefault(item.getId(), 0);
            total += item.getPrice() * qty;
        }
        tvTotalPrice.setText("Total: ₹" + total);
    }

}
