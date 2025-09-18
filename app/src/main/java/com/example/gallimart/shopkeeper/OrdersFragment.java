package com.example.gallimart.shopkeeper;

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
        View view = inflater.inflate(R.layout.fragment_orders_shopkeeper, container, false);

        recyclerView = view.findViewById(R.id.recyclerOrdersShopkeeper);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderList = new ArrayList<>();
        orderList.add(new Order("Order #1", "Pending", "Buyer A"));
        orderList.add(new Order("Order #2", "Delivered", "Buyer B"));

        adapter = new OrdersAdapter(orderList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    public static class Order {
        String id, status, buyer;

        public Order(String id, String status, String buyer) {
            this.id = id;
            this.status = status;
            this.buyer = buyer;
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

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderIdShopkeeper);
                tvStatus = itemView.findViewById(R.id.tvOrderStatusShopkeeper);
                tvBuyer = itemView.findViewById(R.id.tvBuyerShopkeeper);
            }

            void bind(Order order) {
                tvOrderId.setText(order.id);
                tvStatus.setText(order.status);
                tvBuyer.setText(order.buyer);
            }
        }
    }
}
