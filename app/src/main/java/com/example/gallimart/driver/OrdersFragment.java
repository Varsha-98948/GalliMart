package com.example.gallimart.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orderList;

    public OrdersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_orders_driver, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrdersDriver);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Sample orders
        orderList = new ArrayList<>();
        orderList.add(new Order("Order #1", "PENDING", "Shop A → User B"));
        orderList.add(new Order("Order #2", "DELIVERED", "Shop C → User D"));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Order model
    public static class Order {
        String id, status, route;

        public Order(String id, String status, String route) {
            this.id = id;
            this.status = status;
            this.route = route;
        }

        public boolean isPending() {
            return "PENDING".equalsIgnoreCase(status);
        }

        public boolean isDelivered() {
            return "DELIVERED".equalsIgnoreCase(status);
        }
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
                    .inflate(R.layout.item_order_driver, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            holder.bind(orders.get(position));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvStatus, tvRoute;
            Button btnAccept, btnReject, btnUploadPhoto, btnMarkDelivered;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                tvRoute = itemView.findViewById(R.id.tvOrderRoute);

                btnAccept = itemView.findViewById(R.id.btnAcceptOrder);
                btnReject = itemView.findViewById(R.id.btnRejectOrder);
                btnUploadPhoto = itemView.findViewById(R.id.btnUploadPhoto);
                btnMarkDelivered = itemView.findViewById(R.id.btnMarkDelivered);
            }

            void bind(Order order) {
                tvOrderId.setText(order.id);
                tvStatus.setText("Status: " + order.status);
                tvRoute.setText(order.route);

                // Show/hide buttons based on order status
                if (order.isPending()) {
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnUploadPhoto.setVisibility(View.GONE);
                    btnMarkDelivered.setVisibility(View.GONE);
                } else if (order.isDelivered()) {
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnUploadPhoto.setVisibility(View.GONE);
                    btnMarkDelivered.setVisibility(View.GONE);
                } else { // Active order in progress
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnUploadPhoto.setVisibility(View.VISIBLE);
                    btnMarkDelivered.setVisibility(View.VISIBLE);
                }

                btnAccept.setOnClickListener(v -> {
                    order.status = "IN_PROGRESS";
                    notifyItemChanged(getAdapterPosition());
                });

                btnReject.setOnClickListener(v -> {
                    order.status = "REJECTED";
                    notifyItemChanged(getAdapterPosition());
                });

                btnUploadPhoto.setOnClickListener(v -> {
                    // TODO: Open photo picker
                });

                btnMarkDelivered.setOnClickListener(v -> {
                    order.status = "DELIVERED";
                    notifyItemChanged(getAdapterPosition());
                });
            }
        }
    }
}
