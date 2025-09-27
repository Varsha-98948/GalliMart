package com.example.gallimart.buyer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallimart.R;
import com.example.gallimart.SessionManager;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnQuantityChangeListener {
        void onQuantityChanged(SessionManager.CartItem item, int newQuantity);
    }

    private final List<SessionManager.CartItem> cartItems;
    private final OnQuantityChangeListener onQuantityChanged;

    public CartAdapter(List<SessionManager.CartItem> cartItems,
                       OnQuantityChangeListener onQuantityChanged) {
        this.cartItems = cartItems;
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
        SessionManager.CartItem item = cartItems.get(position);

        holder.tvName.setText(item.name);
        holder.tvPrice.setText("₹" + item.price);
        holder.tvQuantity.setText(String.valueOf(item.quantity));

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.imageUrl)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_inventory);
        }

        holder.btnIncrease.setOnClickListener(v -> onQuantityChanged.onQuantityChanged(item, item.quantity + 1));
        holder.btnDecrease.setOnClickListener(v -> onQuantityChanged.onQuantityChanged(item, item.quantity - 1));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity;
        ImageButton btnIncrease, btnDecrease;
        ImageView ivImage;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncreaseQty);
            btnDecrease = itemView.findViewById(R.id.btnDecreaseQty);
            ivImage = itemView.findViewById(R.id.ivCartItem);
        }
    }
}
