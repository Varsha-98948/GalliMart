package com.example.gallimart.buyer;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.Order;
import com.example.gallimart.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class BuyerOrdersAdapter extends RecyclerView.Adapter<BuyerOrdersAdapter.ViewHolder> {

    private final List<Order> orderList;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public BuyerOrdersAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buyer_order, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final Order order = orderList.get(position);
        if (order == null) return;

        // === SHOP NAME ===
        if (order.shopId != null && !order.shopId.isEmpty()) {
            DatabaseReference shopRef = FirebaseDatabase.getInstance()
                    .getReference("shops")
                    .child(order.shopId);

            // Use addListenerForSingleValueEvent to avoid memory leaks
            shopRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String shopName = snapshot.child("name").getValue(String.class);
                    if (shopName != null && !shopName.isEmpty()) {
                        holder.txtShop.setText("Shop: " + shopName);
                    } else {
                        holder.txtShop.setText("Shop ID: " + order.shopId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("BuyerOrdersAdapter", "Failed to fetch shop name", error.toException());
                    holder.txtShop.setText("Shop ID: " + order.shopId);
                }
            });
        } else {
            holder.txtShop.setText("Unknown Shop");
        }

        // === STATUS ===
        String statusText = (order.status != null && !order.status.isEmpty())
                ? order.status
                : "Pending";
        holder.txtStatus.setText("Status: " + statusText);

        // === ITEMS ===
        if (order.items != null && !order.items.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (com.example.gallimart.SessionManager.CartItem item : order.items) {
                sb.append(item.name)
                        .append(" (x")
                        .append(item.quantity)
                        .append("), ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2); // Remove last comma
            holder.txtItems.setText(sb.toString());
        } else {
            holder.txtItems.setText("No items");
        }

        // === CLICK LISTENER ===
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });

        // === ANIMATION ===
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(50f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(position * 40L)
                .start();
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtShop, txtStatus, txtItems;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtShop = itemView.findViewById(R.id.txtShop);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtItems = itemView.findViewById(R.id.txtItems);
        }
    }
}
