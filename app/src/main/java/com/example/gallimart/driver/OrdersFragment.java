package com.example.gallimart.driver;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.gallimart.R;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orderList;

    public OrdersFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_driver, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrdersDriver);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderList = new ArrayList<>();
        orderList.add(new Order("Order #1", "Pending", "From Shop A → User B"));
        orderList.add(new Order("Order #2", "Delivered", "From Shop C → User D"));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    public static class Order {
        String id, status, route;

        public Order(String id, String status, String route) {
            this.id = id;
            this.status = status;
            this.route = route;
        }
    }

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
            Order order = orders.get(position);
            holder.bind(order);
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvOrderId, tvStatus, tvRoute;
            private final Button btnConfirm;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                tvRoute = itemView.findViewById(R.id.tvOrderRoute);
                btnConfirm = itemView.findViewById(R.id.btnConfirmDelivery);
            }

            void bind(Order order) {
                tvOrderId.setText(order.id);
                tvStatus.setText(order.status);
                tvRoute.setText(order.route);

                btnConfirm.setOnClickListener(v -> {
                    // Handle delivery confirmation
                    tvStatus.setText("Delivered");
                    btnConfirm.setEnabled(false);
                });
            }
        }
    }
}
