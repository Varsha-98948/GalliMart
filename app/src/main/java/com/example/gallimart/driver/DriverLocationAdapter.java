package com.example.gallimart.driver.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;
import com.example.gallimart.driver.DriverLocation;

import java.util.List;

public class DriverLocationAdapter extends RecyclerView.Adapter<DriverLocationAdapter.ViewHolder> {

    private List<DriverLocation> locationList;

    public DriverLocationAdapter(List<DriverLocation> locationList) {
        this.locationList = locationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DriverLocation location = locationList.get(position);
        holder.tvPickup.setText(location.getPickup());
        holder.tvDelivery.setText(location.getDelivery());
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDelivery;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDelivery = itemView.findViewById(R.id.tvDelivery);
        }
    }
}
