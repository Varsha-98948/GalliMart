package com.example.gallimart.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private LinearLayout optionOrders, optionHelp;
    private Button btnLogout;
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
        optionHelp = view.findViewById(R.id.optionHelp);
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
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BuyerOrdersFragment())
                    .addToBackStack(null)
                    .commit();
        });

        optionHelp.setOnClickListener(v -> {
            String email = "gallimart.contact@gmail.com";
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gallimart Support");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Gallimart team,");
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
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
        fadeIn(optionHelp, delay + 1000);
        fadeIn(btnLogout, delay + 1200);
    }

    private void fadeIn(View view, int delay) {
        if (view == null) return;
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setStartDelay(delay)
                .setDuration(400)
                .start();
    }
}
