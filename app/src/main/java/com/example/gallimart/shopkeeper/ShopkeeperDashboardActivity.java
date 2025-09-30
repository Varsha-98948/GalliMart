package com.example.gallimart.shopkeeper;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.gallimart.R;
import com.example.gallimart.SessionManager;
import com.example.gallimart.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ShopkeeperDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_shopkeeper_dashboard);

        toolbar = findViewById(R.id.topAppBarShopkeeper);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        bottomNav = findViewById(R.id.bottom_nav);

        // Load default fragment
        loadFragment(new InventoryFragment(), "Inventory", false);

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            String title = "";
            int id = item.getItemId();

            if (id == R.id.nav_inventory) {
                selected = new InventoryFragment();
                title = "Inventory";
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
                title = "Profile";
            } else if (id == R.id.nav_orders) {
                selected = new OrdersFragment();
                title = "Orders";
            } else if (id == R.id.nav_returns) {
                selected = new ReturnsFragment();
                title = "Returns";
            }

            return loadFragment(selected, title, true);
        });
    }

    /**
     * Replaces the current fragment with optional fade animation
     *
     * @param fragment The fragment to display
     * @param title    The toolbar title to set
     * @param animate  Whether to use fade animation
     * @return true if fragment loaded
     */
    private boolean loadFragment(Fragment fragment, String title, boolean animate) {
        if (fragment != null) {
            if (animate) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out)
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }

            return true;
        }
        return false;
    }
}
