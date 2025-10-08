package com.example.gallimart.driver;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DriverDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        toolbar = findViewById(R.id.toolbarDriver);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));

        bottomNav = findViewById(R.id.bottom_nav);

        // Default fragment
        loadFragment(new LocationFragment(), "Locations");

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            String title;

            int id = item.getItemId();
            if (id == R.id.nav_locations) {
                fragment = new LocationFragment();
                title = "Locations";
            } else if (id == R.id.nav_orders) {
                fragment = new OrdersFragment();
                title = "Orders";
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
                title = "Profile";
            } else {
                fragment = new LocationFragment();
                title = "Locations";
            }

            switchFragment(fragment, title);
            return true;
        });
    }

    private void switchFragment(Fragment fragment, String title) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            toolbar.setTitle(title);
        }
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        toolbar.setTitle(title);
    }
}
