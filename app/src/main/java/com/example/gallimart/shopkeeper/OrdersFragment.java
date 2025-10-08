package com.example.gallimart.shopkeeper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private DatabaseReference ordersRef;
    private String shopId;

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
            DatabaseReference shopsRef = FirebaseDatabase.getInstance().getReference("shops");
            shopsRef.orderByChild("email").equalTo(user.getEmail())
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
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
                        public void onCancelled(@NonNull DatabaseError error) {}
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
                            adapter.notifyItemInserted(orderList.size() - 1);
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
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
        private final List<Order> orders;

        OrdersAdapter(List<Order> orders) { this.orders = orders; }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_shopkeeper, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            holder.bind(orders.get(position));
            runFadeInAnimation(holder.itemView, position);
        }

        @Override
        public int getItemCount() { return orders.size(); }

        private void runFadeInAnimation(View view, int position) {
            Animation anim = new AlphaAnimation(0f, 1f);
            anim.setStartOffset(position * 100);
            anim.setDuration(300);
            view.startAnimation(anim);
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvBuyer, tvOrderId, tvStatus;
            private final Button btnAccept, btnReject;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBuyer = itemView.findViewById(R.id.tvBuyerShopkeeper);
                tvOrderId = itemView.findViewById(R.id.tvOrderIdShopkeeper);
                tvStatus = itemView.findViewById(R.id.tvOrderStatusShopkeeper);
                btnAccept = itemView.findViewById(R.id.btnAcceptOrder);
                btnReject = itemView.findViewById(R.id.btnRejectOrder);
            }

            void bind(Order order) {
                // Fetch buyer name from Firebase
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                usersRef.child(order.buyerId).child("name")
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String buyerName = snapshot.getValue(String.class);
                                tvBuyer.setText(buyerName != null ? buyerName : "Unknown Buyer");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                tvBuyer.setText("Unknown Buyer");
                            }
                        });

                tvOrderId.setText("Order ID: " + order.orderId);

                // Status badge
                switch (order.status.toUpperCase()) {
                    case "CONFIRMED":
                        tvStatus.setText("CONFIRMED");
                        tvStatus.setBackgroundResource(R.drawable.status_confirmed);
                        break;
                    case "CANCELED":
                        tvStatus.setText("CANCELED");
                        tvStatus.setBackgroundResource(R.drawable.status_canceled);
                        break;
                    default:
                        tvStatus.setText("PENDING");
                        tvStatus.setBackgroundResource(R.drawable.status_pending);
                        break;
                }

                btnAccept.setOnClickListener(v -> updateOrderStatus(order, "CONFIRMED"));
                btnReject.setOnClickListener(v -> updateOrderStatus(order, "CANCELED"));
            }


        }
    }

    private void updateOrderStatus(Order order, String newStatus) {
        ordersRef.child(order.orderId).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Order " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update order", Toast.LENGTH_SHORT).show());
    }
}
