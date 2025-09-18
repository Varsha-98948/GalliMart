package com.example.gallimart.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole;
    private Button btnLogout;
    private SessionManager sessionManager;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_driver, container, false);

        sessionManager = new SessionManager(getContext());

        tvName = view.findViewById(R.id.tvNameDriver);
        tvEmail = view.findViewById(R.id.tvEmailDriver);
        tvRole = view.findViewById(R.id.tvRoleDriver);
        btnLogout = view.findViewById(R.id.btnLogoutDriver);

        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
        tvRole.setText("Driver");

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout(true);
            startActivity(new Intent(getContext(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }
}
