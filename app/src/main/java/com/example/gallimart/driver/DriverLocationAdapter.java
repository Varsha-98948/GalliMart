package com.example.gallimart.driver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.R;

import java.util.List;

public class DriverLocationAdapter extends RecyclerView.Adapter<DriverLocationAdapter.ViewHolder> {

    private List<DriverLocation> locationList;
    private String orderId = null;
    private String driverStatus = null;
    private Double shopLat = null, shopLng = null, buyerLat = null, buyerLng = null;
    private double routeDistanceKm = 0.0;

    public interface OnItemActionListener {
        void onAccept(String orderId);
        void onReject(String orderId);
        void onUploadPhoto(String orderId);
        void onMarkDelivered(String orderId);
        void onDirections(String orderId, Double lat, Double lng);
        void onShowRoute(String orderId, Double shopLat, Double shopLng, Double buyerLat, Double buyerLng);
    }

    private final OnItemActionListener listener;

    public DriverLocationAdapter(List<DriverLocation> locationList, OnItemActionListener listener) {
        this.locationList = locationList;
        this.listener = listener;
    }

    // updated signature: added routeDistanceKm
    public void setOrderContext(String orderId, String driverStatus, Double shopLat, Double shopLng, Double buyerLat, Double buyerLng, double routeDistanceKm) {
        this.orderId = orderId;
        this.driverStatus = driverStatus;
        this.shopLat = shopLat;
        this.shopLng = shopLng;
        this.buyerLat = buyerLat;
        this.buyerLng = buyerLng;
        this.routeDistanceKm = routeDistanceKm;
        notifyDataSetChanged();
    }

    public void clearOrderContext() {
        this.orderId = null;
        this.driverStatus = null;
        this.shopLat = this.shopLng = this.buyerLat = this.buyerLng = null;
        this.routeDistanceKm = 0.0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DriverLocationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverLocationAdapter.ViewHolder holder, int position) {
        DriverLocation location = locationList.get(position);
        holder.tvPickup.setText(location.getPickup());
        holder.tvDelivery.setText(location.getDelivery());

        // Default hide everything if no order context
        if (orderId == null) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnUpload.setVisibility(View.GONE);
            holder.btnMarkDelivered.setVisibility(View.GONE);
            holder.btnDirections.setVisibility(View.GONE);
            holder.btnShowRoute.setVisibility(View.GONE);
            holder.tvDistance.setVisibility(View.GONE);
            return;
        }

        // Show distance if available
        if (routeDistanceKm > 0) {
            holder.tvDistance.setText(String.format("Approx: %.2f km", routeDistanceKm));
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // If driver has accepted -> show delivery actions, else show accept/reject
        if ("ACCEPTED".equalsIgnoreCase(driverStatus)) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

            holder.btnUpload.setVisibility(View.VISIBLE);
            holder.btnMarkDelivered.setVisibility(View.VISIBLE);
            holder.btnDirections.setVisibility(View.VISIBLE);
            holder.btnShowRoute.setVisibility(View.VISIBLE);
        } else {
            // if driverStatus exists and someone else accepted -> hide buttons & indicate accepted by someone else
            if ("CONFIRMED".equalsIgnoreCase(driverStatus) && orderId != null) {
                // shouldn't happen often because fragment clears, but safe guard
                holder.btnAccept.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
                holder.btnUpload.setVisibility(View.GONE);
                holder.btnMarkDelivered.setVisibility(View.GONE);
                holder.btnDirections.setVisibility(View.GONE);
                holder.btnShowRoute.setVisibility(View.GONE);
            } else {
                holder.btnAccept.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);

                holder.btnUpload.setVisibility(View.GONE);
                holder.btnMarkDelivered.setVisibility(View.GONE);
                holder.btnDirections.setVisibility(View.GONE);
                holder.btnShowRoute.setVisibility(View.GONE);
            }
        }

        // Hook up clicks -> operate on orderId (all items belong to same order)
        holder.btnAccept.setOnClickListener(v -> listener.onAccept(orderId));
        holder.btnReject.setOnClickListener(v -> listener.onReject(orderId));
        holder.btnUpload.setOnClickListener(v -> listener.onUploadPhoto(orderId));
        holder.btnMarkDelivered.setOnClickListener(v -> listener.onMarkDelivered(orderId));
        holder.btnDirections.setOnClickListener(v -> listener.onDirections(orderId, buyerLat, buyerLng));
        holder.btnShowRoute.setOnClickListener(v -> listener.onShowRoute(orderId, shopLat, shopLng, buyerLat, buyerLng));
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDelivery, tvDistance;
        Button btnAccept, btnReject, btnUpload, btnMarkDelivered, btnDirections, btnShowRoute;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDelivery = itemView.findViewById(R.id.tvDelivery);
            tvDistance = itemView.findViewById(R.id.tvDistance);

            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnUpload = itemView.findViewById(R.id.btnUploadPhotoItem);
            btnMarkDelivered = itemView.findViewById(R.id.btnMarkDeliveredItem);
            btnDirections = itemView.findViewById(R.id.btnDirectionsItem);
            btnShowRoute = itemView.findViewById(R.id.btnShowRouteItem);
        }
    }
}
