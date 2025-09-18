package com.example.gallimart.buyer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ItemViewHolder> {

    private final List<Item> itemList;
    private final Map<String, Integer> localQuantities = new HashMap<>();
    private final InventoryFragment fragment;
    private final SessionManager session;

    public InventoryAdapter(List<Item> itemList, InventoryFragment fragment, SessionManager session) {
        this.itemList = itemList;
        this.fragment = fragment;
        this.session = session;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buyer_inventory, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        if (item == null || item.getId() == null) return;

        String itemId = item.getId();
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("₹" + item.getPrice());
        holder.tvQuantity.setText(String.valueOf(localQuantities.getOrDefault(itemId, 0)));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_inventory)
                    .into(holder.ivImage);
        } else holder.ivImage.setImageResource(R.drawable.ic_inventory);

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = localQuantities.getOrDefault(itemId, 0) + 1;
            localQuantities.put(itemId, newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));

            session.addItemToCart(itemId, item.getName(), item.getPrice(), item.getImageUrl());

            Snackbar.make(holder.itemView, item.getName() + " added to cart", Snackbar.LENGTH_SHORT)
                    .setAction("Go to Cart", view -> fragment.navigateToCart())
                    .show();

            fragment.updateCartBadge();
            fragment.navigateToCart(); // immediately go to cart
        });

        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = localQuantities.getOrDefault(itemId, 0);
            if (currentQty > 0) {
                int newQty = currentQty - 1;
                localQuantities.put(itemId, newQty);
                holder.tvQuantity.setText(String.valueOf(newQty));
                session.removeItemFromCart(itemId);
                fragment.updateCartBadge();
            } else Toast.makeText(holder.itemView.getContext(),
                    "Quantity cannot be less than 0", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvQuantity;
        ImageButton btnPlus, btnMinus;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            tvName = itemView.findViewById(R.id.tvItemNameId);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}
