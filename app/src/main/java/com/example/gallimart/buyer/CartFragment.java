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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());
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
        cartAdapter = new CartAdapter(cartItems, this::onQuantityChanged);
        recyclerView.setAdapter(cartAdapter);

        loadCartItems();
        updateTotalPrice();

        return view;
    }

    private void loadCartItems() {
        cartItems.clear();
        Map<String, SessionManager.CartItem> savedCart = session.getCart();
        if (savedCart != null) {
            cartItems.addAll(savedCart.values());
        }
        cartAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        double total = 0;
        for (SessionManager.CartItem item : cartItems) {
            total += item.price * item.quantity;
        }
        tvTotalPrice.setText("Total: ₹" + total);
    }

    private void onQuantityChanged(SessionManager.CartItem item, int newQuantity) {
        Map<String, SessionManager.CartItem> cart = session.getCart();
        if (cart == null) return;

        if (newQuantity <= 0) {
            cart.remove(item.id);
        } else {
            item.quantity = newQuantity;
            cart.put(item.id, item);
        }

        session.saveCart(cart);
        loadCartItems();
        updateTotalPrice();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCartItems();
        updateTotalPrice();
    }
}
