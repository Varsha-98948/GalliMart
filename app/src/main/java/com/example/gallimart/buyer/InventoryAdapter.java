package com.example.gallimart.buyer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallimart.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ItemViewHolder> {

    private final List<Item> itemList;
    private final Map<String, Integer> cartQuantities = new HashMap<>();
    private final InventoryFragment fragment; // reference to fragment

    public InventoryAdapter(List<Item> itemList, InventoryFragment fragment) {
        this.itemList = itemList;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buyer_inventory, parent, false);
        return new ItemViewHolder(view);
    }

    private void navigateToCart(View view) {
        Fragment cartFragment = new CartFragment(); // Make sure this is your cart fragment class
        FragmentManager fragmentManager = ((AppCompatActivity)view.getContext()).getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cartFragment) // container ID in your activity layout
                .addToBackStack(null) // so back button works
                .commit();
    }


    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        if (item == null || item.getId() == null) return;

        String itemId = item.getId();
        holder.tvName.setText(item.getName() != null ? item.getName() : "Item");
        holder.tvPrice.setText("₹" + item.getPrice());

        int quantity = cartQuantities.getOrDefault(itemId, 0);
        holder.tvQuantity.setText(String.valueOf(quantity));

        // Load image from Supabase URL
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_inventory)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_inventory);
        }

        // Add to cart + show popup
        holder.btnPlus.setOnClickListener(v -> {
            int qty = cartQuantities.getOrDefault(itemId, 0) + 1;
            cartQuantities.put(itemId, qty);
            holder.tvQuantity.setText(String.valueOf(qty));

            // Show Snackbar with "Go to Cart" action
            Snackbar.make(holder.itemView, item.getName() + " added to cart", Snackbar.LENGTH_LONG)
                    .setAction("Go to Cart", view -> {
                        // Navigate to Cart fragment
                        navigateToCart(holder.itemView);
                    })
                    .show();
        });

        // Decrease quantity
        holder.btnMinus.setOnClickListener(v -> {
            int qty = cartQuantities.getOrDefault(itemId, 0);
            if (qty > 0) {
                qty--;
                cartQuantities.put(itemId, qty);
                holder.tvQuantity.setText(String.valueOf(qty));
            } else {
                Toast.makeText(holder.itemView.getContext(),
                        "Quantity cannot be less than 0", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

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

    public Map<String, Integer> getCartQuantities() {
        return cartQuantities;
    }
}
