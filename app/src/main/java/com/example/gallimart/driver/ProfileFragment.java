package com.example.gallimart.driver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private Button btnLogout;
    private CardView cardDeliveryHistory, cardHelp;
    private View ivProfile;
    private SessionManager sessionManager;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile_driver, container, false);

        sessionManager = new SessionManager(getContext());

        ivProfile = view.findViewById(R.id.ivProfileDriver);
        tvName = view.findViewById(R.id.tvNameDriver);
        tvEmail = view.findViewById(R.id.tvEmailDriver);
        tvRole = view.findViewById(R.id.tvRoleDriver);
        btnLogout = view.findViewById(R.id.btnLogoutDriver);
        cardDeliveryHistory = view.findViewById(R.id.cardDeliveryHistory);
        cardHelp = view.findViewById(R.id.cardHelpDriver);

        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
        tvRole.setText("Driver");

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout(true);
            startActivity(new Intent(getContext(), com.example.gallimart.login.LoginActivity.class));
            getActivity().finish();
        });

        // Delivery History → Fragment_Orders_Driver
        cardDeliveryHistory.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new OrdersFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Help → Email Intent
        cardHelp.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@gallimart.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Driver Support");
            startActivity(Intent.createChooser(emailIntent, "Contact Support"));
        });

        // Run fade-in animations sequentially
        animateFadeIn(ivProfile, 0);
        animateFadeIn(tvName, 150);
        animateFadeIn(tvEmail, 300);
        animateFadeIn(tvRole, 450);
        animateFadeIn(cardDeliveryHistory, 600);
        animateFadeIn(cardHelp, 750);
        animateFadeIn(btnLogout, 900);

        return view;
    }

    private void animateFadeIn(View view, int delayMillis) {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        fadeIn.setStartOffset(delayMillis);
        view.startAnimation(fadeIn);
    }
}
