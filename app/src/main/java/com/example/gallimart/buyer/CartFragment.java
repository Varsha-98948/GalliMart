package com.example.gallimart.buyer;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;


import com.airbnb.lottie.LottieAnimationView;
import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvTotalPrice;
    private CartAdapter cartAdapter;
    private final List<SessionManager.CartItem> cartItems = new ArrayList<>();
    private SessionManager session;
    private double totalPrice = 0;
    private double deliveryCost = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference ordersRef;
    private LottieAnimationView lottieEmpty;
    private LinearLayout checkoutContainer;

    private final SessionManager.CartChangeListener cartChangeListener = () -> {
        loadCartItems();
        updateTotalPrice();
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(requireContext());
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
        lottieEmpty = view.findViewById(R.id.lottieEmptyCart);
        checkoutContainer = view.findViewById(R.id.checkoutContainer);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(cartItems, this::handleQuantityChange);
        recyclerView.setAdapter(cartAdapter);

        loadCartItems();
        updateTotalPrice();

        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        view.findViewById(R.id.btnCheckout).setOnClickListener(v -> checkout());

        return view;
    }

    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
        shopsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String shopId = null;

                for (DataSnapshot shopSnap : snapshot.getChildren()) {
                    if (shopSnap.hasChild("items")) {
                        DataSnapshot itemsSnap = shopSnap.child("items");
                        for (SessionManager.CartItem cartItem : cartItems) {
                            if (itemsSnap.hasChild(cartItem.id)) {
                                shopId = shopSnap.getKey();
                                break;
                            }
                        }
                    }
                    if (shopId != null) break;
                }

                if (shopId == null) {
                    Toast.makeText(getContext(), "Cannot find shop for cart items!", Toast.LENGTH_SHORT).show();
                    return;
                }

                calculateDeliveryCost(shopId, () -> showInvoiceDialog());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to get shopId: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInvoiceDialog() {
        double grandTotal = totalPrice + deliveryCost;

        StringBuilder invoice = new StringBuilder();
        for (SessionManager.CartItem item : cartItems) {
            invoice.append(item.name)
                    .append(" (x").append(item.quantity).append(")")
                    .append(" - ₹").append(item.price * item.quantity).append("\n");
        }
        invoice.append("\nDelivery Cost: ₹").append(deliveryCost)
                .append("\n----------------------\n")
                .append("Total Payable: ₹").append(grandTotal);

        new AlertDialog.Builder(requireContext())
                .setTitle("Invoice Summary")
                .setMessage(invoice.toString())
                .setPositiveButton("Proceed to Pay", (dialog, which) -> {
                    totalPrice = grandTotal; // include delivery in total
                    processPayment();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }




    // --- Delivery Cost Calculation ---
    private void calculateDeliveryCost(String shopId, Runnable onComplete) {
        DatabaseReference shopRef = FirebaseDatabase.getInstance().getReference("shops").child(shopId);

        shopRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double shopLat = snapshot.child("lat").getValue(Double.class);
                Double shopLng = snapshot.child("lng").getValue(Double.class);

                if (shopLat == null || shopLng == null) {
                    deliveryCost = 0;
                    if (onComplete != null) onComplete.run();
                    return;
                }

                if (requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED &&
                        requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {

                    // Permission not granted — skip delivery cost or set default
                    deliveryCost = 0;
                    if (onComplete != null) onComplete.run();
                    return;
                }

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                double buyerLat = location.getLatitude();
                                double buyerLng = location.getLongitude();

                                double distanceKm = calculateDistance(shopLat, shopLng, buyerLat, buyerLng);
                                deliveryCost = getDeliveryCharge(distanceKm);
                            } else {
                                deliveryCost = 0;
                            }
                            if (onComplete != null) onComplete.run();
                        })
                        .addOnFailureListener(e -> {
                            deliveryCost = 0;
                            if (onComplete != null) onComplete.run();
                        });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                deliveryCost = 0;
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double getDeliveryCharge(double distanceKm) {
        if (distanceKm < 2) return 20;
        else if (distanceKm < 4) return 40;
        else if (distanceKm < 7) return 60;
        else return 0;
    }


    private void processPayment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
        shopsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String shopId = null;

                for (DataSnapshot shopSnap : snapshot.getChildren()) {
                    if (shopSnap.hasChild("items")) {
                        DataSnapshot itemsSnap = shopSnap.child("items");
                        for (SessionManager.CartItem cartItem : cartItems) {
                            if (itemsSnap.hasChild(cartItem.id)) {
                                shopId = shopSnap.getKey();
                                break;
                            }
                        }
                    }
                    if (shopId != null) break;
                }

                if (shopId == null) {
                    Toast.makeText(getContext(), "Cannot find shop for cart items!", Toast.LENGTH_SHORT).show();
                    return;
                }

                placeOrder(user, shopId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to get shopId: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void placeOrder(FirebaseUser user, String shopId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String buyerName = snapshot.child("name").getValue(String.class);
                if (buyerName == null || buyerName.isEmpty()) {
                    buyerName = user.getEmail(); // fallback
                }

                String orderId = "ORD_" + System.currentTimeMillis();
                Order order = new Order(
                        orderId,
                        user.getUid(),
                        buyerName,
                        shopId,
                        new ArrayList<>(cartItems),
                        totalPrice,
                        "PLACED",
                        "",
                        System.currentTimeMillis(),
                        deliveryCost
                );

                saveOrderToBackend(order);
                session.clearCart();
                refreshCart();
                Toast.makeText(getContext(), "Payment Successful! Order Placed.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch user name: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void saveOrderToBackend(Order order) {
        ordersRef.child(order.orderId)
                .setValue(order)
                .addOnSuccessListener(aVoid -> Log.d("CartFragment", "Order saved: " + order.orderId))
                .addOnFailureListener(e -> Log.e("CartFragment", "Failed to save order", e));
    }

    private void loadCartItems() {
        cartItems.clear();
        cartItems.addAll(session.getCart().values());
        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();

        // Show/hide empty cart animation
        if (cartItems.isEmpty()) {
            lottieEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            checkoutContainer.setVisibility(View.GONE);
        } else {
            lottieEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            checkoutContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateTotalPrice() {
        totalPrice = 0;
        for (SessionManager.CartItem item : cartItems) {
            totalPrice += item.price * item.quantity;
        }
        tvTotalPrice.setText("Total: ₹" + totalPrice);
    }

    private void handleQuantityChange(SessionManager.CartItem item, int newQuantity) {
        if (newQuantity <= 0) {
            session.removeItemFromCart(item.id);
        } else {
            SessionManager.CartItem updated =
                    new SessionManager.CartItem(item.id, item.name, item.price, item.imageUrl, newQuantity);
            Map<String, SessionManager.CartItem> cart = session.getCart();
            cart.put(item.id, updated);
            session.saveCart(cart);
        }
    }

    public void refreshCart() {
        loadCartItems();
        updateTotalPrice();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        session.removeCartChangeListener(cartChangeListener);
    }
}
