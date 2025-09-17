package com.example.gallimart.buyer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;

import java.util.List;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<Item> items;
    private final Map<String, Integer> cartQuantities;
    private final Runnable onQuantityChanged;

    public CartAdapter(List<Item> items, Map<String, Integer> cartQuantities, Runnable onQuantityChanged) {
        this.items = items;
        this.cartQuantities = cartQuantities;
        this.onQuantityChanged = onQuantityChanged;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        Item item = items.get(position);
        int qty = cartQuantities.getOrDefault(item.getId(), 0);

        holder.tvItemName.setText(item.getName());
        holder.tvItemPrice.setText("₹" + item.getPrice());
        holder.tvQuantity.setText(String.valueOf(qty));

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = cartQuantities.getOrDefault(item.getId(), 0) + 1;
            cartQuantities.put(item.getId(), newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            onQuantityChanged.run();
        });

        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = cartQuantities.getOrDefault(item.getId(), 0);
            if (currentQty > 0) {
                currentQty--;
                cartQuantities.put(item.getId(), currentQty);
                holder.tvQuantity.setText(String.valueOf(currentQty));
                onQuantityChanged.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPrice, tvQuantity;
        ImageButton btnPlus, btnMinus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvCartItemName);
            tvItemPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartItemQuantity);
            btnPlus = itemView.findViewById(R.id.btnCartPlus);
            btnMinus = itemView.findViewById(R.id.btnCartMinus);
        }
    }
}
