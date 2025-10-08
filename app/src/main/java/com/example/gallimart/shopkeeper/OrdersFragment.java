package com.example.gallimart.shopkeeper;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private DatabaseReference ordersRef;
    private String shopId;

    public OrdersFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_shopkeeper, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrdersShopkeeper);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Fetch shopId based on shopkeeper email
            DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
            shopsRef.orderByChild("email").equalTo(user.getEmail())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot shopSnap : snapshot.getChildren()) {
                                    shopId = shopSnap.getKey();
                                    listenForOrders();
                                    break;
                                }
                            } else {
                                Toast.makeText(getContext(), "No shop found for this user", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Failed to get shopId: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        return view;
    }

    private void listenForOrders() {
        if (shopId == null) return;

        ordersRef.orderByChild("shopId").equalTo(shopId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                        Order order = snapshot.getValue(Order.class);
                        if (order != null) {
                            orderList.add(order);
                            adapter.notifyDataSetChanged();

                            // Only show Toast if fragment is attached
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "New Order: " + order.orderId, Toast.LENGTH_LONG).show();
                            }
                        }
                    }


                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                        Order updated = snapshot.getValue(Order.class);
                        if (updated != null) {
                            for (int i = 0; i < orderList.size(); i++) {
                                if (orderList.get(i).orderId.equals(updated.orderId)) {
                                    orderList.set(i, updated);
                                    adapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    }

                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("OrdersFragment", "Firebase error: ", error.toException());
                    }
                });
    }

    // Adapter
    public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
        private final List<Order> orders;

        public OrdersAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_shopkeeper, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.bind(order);
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvOrderId, tvStatus, tvBuyer;
            private final Button btnAccept, btnReject;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderIdShopkeeper);
                tvStatus = itemView.findViewById(R.id.tvOrderStatusShopkeeper);
                tvBuyer = itemView.findViewById(R.id.tvBuyerShopkeeper);
                btnAccept = itemView.findViewById(R.id.btnAcceptOrder);
                btnReject = itemView.findViewById(R.id.btnRejectOrder);
            }

            void bind(Order order) {
                tvOrderId.setText(order.orderId);
                tvStatus.setText(order.status);
                tvBuyer.setText(order.buyerName);

                btnAccept.setOnClickListener(v -> updateOrderStatus(order, "CONFIRMED"));
                btnReject.setOnClickListener(v -> updateOrderStatus(order, "CANCELED"));
            }
        }
    }

    // Update order status and assign driver if accepted
    private void updateOrderStatus(Order order, String newStatus) {
        ordersRef.child(order.orderId).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Order " + newStatus, Toast.LENGTH_SHORT).show();

                    if (newStatus.equals("CONFIRMED")) { // Update inventory only when confirmed
                        updateInventoryInFirebase(order);
                    }

                    if (newStatus.equals("CONFIRMED")) {
                        // Fetch shop location for driver assignment
                        DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
                        shopsRef.child(order.shopId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Double shopLat = snapshot.child("lat").getValue(Double.class);
                                    Double shopLng = snapshot.child("lng").getValue(Double.class);
                                    if (shopLat != null && shopLng != null) {
                                        assignDriverToOrder(order, shopLat, shopLng);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update order", Toast.LENGTH_SHORT).show());
    }

    private void updateInventoryInFirebase(Order order) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                .getReference("shops")
                .child(order.shopId)
                .child("items");

        // Fetch order items directly from Firebase
        ordersRef.child(order.orderId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        for (DataSnapshot orderItemSnap : snapshot.getChildren()) {
                            String itemId = orderItemSnap.child("id").getValue(String.class);
                            Long orderedQtyLong = orderItemSnap.child("quantity").getValue(Long.class);
                            int orderedQty = orderedQtyLong != null ? orderedQtyLong.intValue() : 0;

                            if (itemId != null && orderedQty > 0) {
                                // Use transaction to safely update stock
                                itemsRef.child(itemId).child("quantity").runTransaction(new com.google.firebase.database.Transaction.Handler() {
                                    @NonNull
                                    @Override
                                    public com.google.firebase.database.Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        Long currentQtyLong = currentData.getValue(Long.class);
                                        int currentQty = currentQtyLong != null ? currentQtyLong.intValue() : 0;
                                        int newQty = currentQty - orderedQty;
                                        if (newQty < 0) newQty = 0; // prevent negative
                                        currentData.setValue(newQty);
                                        return com.google.firebase.database.Transaction.success(currentData);
                                    }

                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                                        if (committed) {
                                            Log.d("InventoryUpdate", "Item " + itemId + " updated successfully.");
                                        } else {
                                            Log.e("InventoryUpdate", "Failed to update item " + itemId + ": " + (error != null ? error.getMessage() : ""));
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to fetch order items: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }




    private void assignDriverToOrder(Order order, double shopLat, double shopLng) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("role").equalTo("Driver")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String nearestDriverId = null;
                        double minDistance = Double.MAX_VALUE;

                        for (DataSnapshot driverSnap : snapshot.getChildren()) {
                            if (!driverSnap.hasChild("address")) continue;

                            Double driverLat = driverSnap.child("address/lat").getValue(Double.class);
                            Double driverLng = driverSnap.child("address/lng").getValue(Double.class);
                            Boolean available = driverSnap.child("available").getValue(Boolean.class);

                            if (driverLat == null || driverLng == null) continue;
                            if (available != null && !available) continue;

                            double distance = distanceInKm(shopLat, shopLng, driverLat, driverLng);
                            if (distance < minDistance) {
                                minDistance = distance;
                                nearestDriverId = driverSnap.getKey();
                            }
                        }

                        if (nearestDriverId != null) {
                            usersRef.child(nearestDriverId).child("currentOrderId").setValue(order.orderId);
                            usersRef.child(nearestDriverId).child("available").setValue(false);
                            Toast.makeText(getContext(), "Order assigned to driver", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "No available drivers nearby", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // Haversine formula
    private double distanceInKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }
}
