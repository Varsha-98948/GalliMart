package com.example.gallimart.driver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DriverDashboardActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        bottomNav = findViewById(R.id.bottom_nav);

        // Default fragment
        loadFragment(new LocationFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.nav_locations) {
                selected = new LocationFragment(); // pickup & delivery locations
            } else if (id == R.id.nav_delivery) {
                selected = new DeliveryFragment(); // delivery confirmation
            } else if (id == R.id.nav_orders) {
                selected = new OrdersFragment(); // order history + payment
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment(); // profile, logout
            }

            return loadFragment(selected);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
