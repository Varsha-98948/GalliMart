package com.example.gallimart.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();

    public OrdersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_driver, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrdersDriver);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        fetchDriverOrders();

        return view;
    }

    private void fetchDriverOrders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String driverId = user.getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // Fetch all shops once
        db.child("shops").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Shop> shopMap = new HashMap<>();
                for (DataSnapshot shopSnap : snapshot.getChildren()) {
                    String shopId = shopSnap.getKey();
                    String shopName = shopSnap.child("name").getValue(String.class);
                    Double shopLat = shopSnap.child("lat").getValue(Double.class);
                    Double shopLng = shopSnap.child("lng").getValue(Double.class);
                    if (shopId != null && shopName != null && shopLat != null && shopLng != null) {
                        shopMap.put(shopId, new Shop(shopName, shopLat, shopLng));
                    }
                }

                // Fetch active and completed orders
                fetchOrdersByType(db.child("orders"), driverId, shopMap, false);
                fetchOrdersByType(db.child("completedOrders"), driverId, shopMap, true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch shops: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOrdersByType(DatabaseReference ordersRef, String driverId, Map<String, Shop> shopMap, boolean completed) {
        ordersRef.orderByChild("driverId").equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            Order order = parseOrder(orderSnap, shopMap, completed);
                            if (order != null) orderList.add(order);
                        }

                        // Sort by timestamp descending
                        Collections.sort(orderList, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to fetch orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Order parseOrder(DataSnapshot snapshot, Map<String, Shop> shopMap, boolean completed) {
        String orderId = snapshot.child("orderId").getValue(String.class);
        String buyerName = snapshot.child("buyerName").getValue(String.class);
        String shopId = snapshot.child("shopId").getValue(String.class);
        Long timestamp = snapshot.child("timestamp").getValue(Long.class);

        Double buyerLat = snapshot.child("driverLocation").child("lat").getValue(Double.class);
        Double buyerLng = snapshot.child("driverLocation").child("lng").getValue(Double.class);

        if (orderId == null || buyerName == null || shopId == null || timestamp == null
                || buyerLat == null || buyerLng == null) return null;

        Shop shop = shopMap.get(shopId);
        if (shop == null) return null;

        String route = shop.name + " → " + buyerName;
        String status = completed ? "DELIVERED" : "IN PROGRESS";

        double distanceKm = calculateDistance(shop.lat, shop.lng, buyerLat, buyerLng);
        String earnings = calculateEarnings(distanceKm);

        return new Order(orderId, status, route, timestamp, earnings);
    }

    // Haversine formula
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // in km
    }

    private String calculateEarnings(double distanceKm) {
        if (distanceKm < 2) return "₹20";
        else if (distanceKm < 4) return "₹40";
        else if (distanceKm < 7) return "₹60";
        else return "₹0";
    }

    // Models
    public static class Shop {
        String name;
        double lat, lng;

        public Shop(String name, double lat, double lng) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static class Order {
        String id, status, route, earnings;
        long timestamp;

        public Order(String id, String status, String route, long timestamp, String earnings) {
            this.id = id;
            this.status = status;
            this.route = route;
            this.timestamp = timestamp;
            this.earnings = earnings;
        }
    }

    // Adapter
    public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
        private final List<Order> orders;

        public OrdersAdapter(List<Order> orders) { this.orders = orders; }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_driver, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            holder.bind(orders.get(position));
        }

        @Override
        public int getItemCount() { return orders.size(); }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvStatus, tvRoute, tvEarnings;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                tvRoute = itemView.findViewById(R.id.tvOrderRoute);
                tvEarnings = itemView.findViewById(R.id.tvOrderEarnings);
            }

            void bind(Order order) {
                tvOrderId.setText(order.id);
                tvStatus.setText(order.status);
                tvRoute.setText(order.route);
                tvEarnings.setText(order.earnings);
            }
        }
    }
}
