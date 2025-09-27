package com.example.gallimart.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvTotalPrice;
    private CartAdapter cartAdapter;
    private final List<SessionManager.CartItem> cartItems = new ArrayList<>();
    private SessionManager session;

    // keep a reference to our listener so we can remove it later
    private final SessionManager.CartChangeListener cartChangeListener = () -> {
        loadCartItems();
        updateTotalPrice();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());

        // ✅ register our listener (multi-listener aware SessionManager)
        session.addCartChangeListener(cartChangeListener);
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
        cartAdapter = new CartAdapter(cartItems, this::handleQuantityChange);
        recyclerView.setAdapter(cartAdapter);

        loadCartItems();
        updateTotalPrice();
        return view;
    }

    private void loadCartItems() {
        cartItems.clear();
        cartItems.addAll(session.getCart().values());
        cartAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        double total = 0;
        for (SessionManager.CartItem item : cartItems) {
            total += item.price * item.quantity;
        }
        tvTotalPrice.setText("Total: ₹" + total);
    }

    private void handleQuantityChange(SessionManager.CartItem item, int newQuantity) {
        if (newQuantity <= 0) {
            session.removeItemFromCart(item.id);
        } else {
            // create a new CartItem with new quantity
            SessionManager.CartItem updated =
                    new SessionManager.CartItem(item.id, item.name, item.price, item.imageUrl, newQuantity);
            Map<String, SessionManager.CartItem> cart = session.getCart();
            cart.put(item.id, updated);
            session.saveCart(cart); // ✅ triggers listener
        }
    }


    public void refreshCart() {
        loadCartItems();
        updateTotalPrice();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ✅ unregister listener to prevent leaks
        session.removeCartChangeListener(cartChangeListener);
    }
}
