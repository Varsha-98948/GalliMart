package com.example.gallimart.buyer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gallimart.R;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    public interface OnShopClickListener {
        void onShopClick(Shop shop);
    }

    private List<Shop> shopList;
    private OnShopClickListener listener;

    public ShopAdapter(List<Shop> shopList, OnShopClickListener listener) {
        this.shopList = shopList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Shop shop = shopList.get(position);
        holder.shopName.setText(shop.getName());

        // Show distance if available, otherwise hide
        if (shop.getDistance() > 0) {
            holder.distance.setText(String.format("%.1f km away", shop.getDistance()));
            holder.distance.setVisibility(View.VISIBLE);
        } else {
            holder.distance.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onShopClick(shop));
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView shopName, distance;

        ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            shopName = itemView.findViewById(R.id.tvShopName);
            distance = itemView.findViewById(R.id.tvDistance);
        }
    }
}
