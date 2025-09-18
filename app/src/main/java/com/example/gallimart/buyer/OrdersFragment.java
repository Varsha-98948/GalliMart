package com.example.gallimart.buyer;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Dummy orders
        orderList = new ArrayList<>();
        orderList.add(new Order("Order #1", "Pending", 250.0));
        orderList.add(new Order("Order #2", "Delivered", 120.0));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Order model
    public static class Order {
        String title, status;
        double total;

        public Order(String title, String status, double total) {
            this.title = title;
            this.status = status;
            this.total = total;
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
                    .inflate(R.layout.item_order, parent, false);
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
            private final TextView tvTitle, tvStatus, tvTotal;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvOrderTitle);
                tvStatus = itemView.findViewById(R.id.tvOrderStatus);
                tvTotal = itemView.findViewById(R.id.tvOrderTotal);
            }

            void bind(Order order) {
                tvTitle.setText(order.title);
                tvStatus.setText(order.status);
                tvTotal.setText("₹" + order.total);
            }
        }
    }
}
