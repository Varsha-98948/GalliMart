package com.example.gallimart.buyer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallimart.Order;
import com.example.gallimart.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BuyerOrdersFragment extends Fragment implements BuyerOrdersAdapter.OnOrderClickListener {

    private static final String TAG = "BuyerOrdersFragment";

    private RecyclerView rvOrders;
    private BuyerOrdersAdapter adapter;
    private List<Order> orderList;

    private DatabaseReference ordersRef;
    private String buyerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buyer_orders, container, false);

        rvOrders = view.findViewById(R.id.rvBuyerOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        orderList = new ArrayList<>();
        adapter = new BuyerOrdersAdapter(orderList, this);
        rvOrders.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }
        buyerId = user.getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        fetchBuyerOrders();

        return view;
    }

    private void fetchBuyerOrders() {
        ordersRef.orderByChild("buyerId").equalTo(buyerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        orderList.clear();
                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            Order order = orderSnap.getValue(Order.class);
                            if (order != null) {
                                order.orderId = orderSnap.getKey(); // store key for later
                                orderList.add(order);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w(TAG, "fetchBuyerOrders cancelled: " + error);
                    }
                });
    }

    @Override
    public void onOrderClick(Order order) {
        // Open BuyerOrderDetailFragment instead of starting an Activity
        BuyerOrderDetailFragment fragment = new BuyerOrderDetailFragment();
        Bundle args = new Bundle();
        args.putString("orderId", order.orderId);
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment) // R.id.fragment_container = your activity's fragment container
                .addToBackStack(null)
                .commit();
    }
}
