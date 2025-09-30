package com.example.gallimart.shopkeeper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.gallimart.R;

import java.util.ArrayList;
import java.util.List;

public class ReturnsFragment extends Fragment {

    private RecyclerView rvReturns;
    private LottieAnimationView animationLoading, animationEmpty;
    private ReturnsAdapter adapter;
    private final List<ReturnItem> returnItems = new ArrayList<>(); // Replace with your model class

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_returns, container, false);

        rvReturns = view.findViewById(R.id.rvReturns);
        animationEmpty = view.findViewById(R.id.animEmptyReturns);

        rvReturns.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReturnsAdapter(returnItems);
        rvReturns.setAdapter(adapter);

        // Simulate data fetching (remove when connecting Firebase)
        fetchReturns();

        return view;
    }

    private void fetchReturns() {

        rvReturns.setVisibility(View.GONE);
        animationEmpty.setVisibility(View.GONE);

        // Simulate a delay (e.g., Firebase fetching)
        rvReturns.postDelayed(() -> {
            // Remove this line because animationLoading is null
            // animationLoading.setVisibility(View.GONE);

            if (returnItems.isEmpty()) {
                animationEmpty.setVisibility(View.VISIBLE);
            } else {
                rvReturns.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }, 2000); // 2-second delay for demo
    }


    // RecyclerView Adapter
    private static class ReturnsAdapter extends RecyclerView.Adapter<ReturnsAdapter.ReturnViewHolder> {
        private final List<ReturnItem> items;

        ReturnsAdapter(List<ReturnItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ReturnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ReturnViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReturnViewHolder holder, int position) {
            ReturnItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ReturnViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.TextView text1, text2;

            ReturnViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }

            void bind(ReturnItem item) {
                text1.setText(item.getOrderId());
                text2.setText(item.getReason());
            }
        }
    }

    // Dummy model for now
    public static class ReturnItem {
        private String orderId;
        private String reason;

        public ReturnItem(String orderId, String reason) {
            this.orderId = orderId;
            this.reason = reason;
        }

        public String getOrderId() { return orderId; }
        public String getReason() { return reason; }
    }
}
