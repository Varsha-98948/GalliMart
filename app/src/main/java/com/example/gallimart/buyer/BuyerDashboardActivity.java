package com.example.gallimart.buyer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gallimart.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;

public class BuyerDashboardActivity extends AppCompatActivity {

    private LocationFragment locationFragment;
    private InventoryFragment inventoryFragment;
    private CartFragment cartFragment;
    private ProfileFragment profileFragment;
    private BuyerOrdersFragment buyerOrderFragment;
    private Fragment activeFragment;
    private BottomNavigationView bottomNavigation;
    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_dashboard);

        topAppBar = findViewById(R.id.topAppBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initialize fragments
        locationFragment = new LocationFragment();
        cartFragment = new CartFragment();
        profileFragment = new ProfileFragment();
        buyerOrderFragment = new BuyerOrdersFragment();
        // inventoryFragment = null initially

        // Add only home, profile, and cart initially
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, locationFragment, "HOME")
                .add(R.id.fragment_container, profileFragment, "PROFILE").hide(profileFragment)
                .add(R.id.fragment_container, cartFragment, "CART").hide(cartFragment)
                .add(R.id.fragment_container, buyerOrderFragment, "ORDERS").hide(buyerOrderFragment)
                .commit();

        activeFragment = locationFragment;
        setToolbarTitle("Home"); // initial title

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragmentToShow = null;
            String title = "";

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragmentToShow = locationFragment;
                title = "Home";
            } else if (id == R.id.nav_inventory) {
                if (inventoryFragment == null) {
                    inventoryFragment = new InventoryFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, inventoryFragment, "INVENTORY")
                            .hide(activeFragment)
                            .commit();
                    activeFragment = inventoryFragment;
                    setToolbarTitle("Inventory");
                    return true;
                } else {
                    fragmentToShow = inventoryFragment;
                    title = "Inventory";
                }
            } else if (id == R.id.nav_cart) {
                fragmentToShow = cartFragment;
                title = "Cart";
            } else if (id == R.id.nav_profile) {
                fragmentToShow = profileFragment;
                title = "Profile";
            } else if (id == R.id.nav_orders) {
                fragmentToShow = buyerOrderFragment;
                title = "Orders";
            }

            if (fragmentToShow != null && fragmentToShow != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(fragmentToShow)
                        .commit();
                activeFragment = fragmentToShow;
                setToolbarTitle(title);
            }
            return true;
        });
    }

    private void setToolbarTitle(String title) {
        if (topAppBar != null) topAppBar.setTitle(title);
    }

    public void switchToCartTab() {
        bottomNavigation.setSelectedItemId(R.id.nav_cart);
    }

    public void showInventoryForShop(String shopId) {
        if (inventoryFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(inventoryFragment)
                    .commit();
        }
        inventoryFragment = InventoryFragment.newInstance(shopId);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, inventoryFragment, "INVENTORY")
                .hide(activeFragment)
                .commit();
        activeFragment = inventoryFragment;
        bottomNavigation.setSelectedItemId(R.id.nav_inventory);
        setToolbarTitle("Inventory");
    }

    public CartFragment getCartFragment() {
        return cartFragment;
    }
}
