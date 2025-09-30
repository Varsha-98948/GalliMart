package com.example.gallimart.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvRole;
    private LinearLayout optionOrders, optionAddress, optionPayment, optionHelp, optionSettings;
    private androidx.appcompat.widget.AppCompatButton btnLogout;
    private SessionManager sessionManager;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(getContext());

        ivProfile = view.findViewById(R.id.ivProfile);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvRole = view.findViewById(R.id.tvRole);

        optionOrders = view.findViewById(R.id.optionOrders);
        optionAddress = view.findViewById(R.id.optionAddress);
        optionPayment = view.findViewById(R.id.optionPayment);
        optionHelp = view.findViewById(R.id.optionHelp);
        optionSettings = view.findViewById(R.id.optionSettings);

        btnLogout = view.findViewById(R.id.btnLogout);

        loadUserDetails();
        setupOptionClicks();
        setupLogout();
        runFadeInAnimations();

        return view;
    }

    private void loadUserDetails() {
        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
        tvRole.setText("Buyer");
    }

    private void setupOptionClicks() {
        optionOrders.setOnClickListener(v -> {
            // Navigate to Orders Fragment / Activity
        });

        optionAddress.setOnClickListener(v -> {
            // Navigate to Address Fragment / Activity
        });

        optionPayment.setOnClickListener(v -> {
            // Navigate to Payment Methods
        });

        optionHelp.setOnClickListener(v -> {
            // Navigate to Help / FAQ
        });

        optionSettings.setOnClickListener(v -> {
            // Navigate to Settings
        });
    }

    private void setupLogout() {
        btnLogout.setOnClickListener(v -> {
            sessionManager.clearCart();
            sessionManager.logout(true);
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    private void runFadeInAnimations() {
        int delay = 200;
        fadeIn(ivProfile, delay);
        fadeIn(tvName, delay + 200);
        fadeIn(tvEmail, delay + 400);
        fadeIn(tvRole, delay + 600);
        fadeIn(optionOrders, delay + 800);
        fadeIn(optionAddress, delay + 1000);
        fadeIn(optionPayment, delay + 1200);
        fadeIn(optionHelp, delay + 1400);
        fadeIn(optionSettings, delay + 1600);
        fadeIn(btnLogout, delay + 1800);
    }

    private void fadeIn(View view, int delay) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setStartDelay(delay)
                .setDuration(400)
                .start();
    }
}
