package com.example.gallimart.buyer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BuyerDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_dashboard);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Redirect to login if not logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottom_nav);

        // Default fragment
        loadFragment(new LocationFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.nav_location) {
                selected = new LocationFragment();
            } else if (id == R.id.nav_cart) {
                selected = new CartFragment();
            } else if (id == R.id.nav_orders) {
                selected = new OrdersFragment();
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
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
