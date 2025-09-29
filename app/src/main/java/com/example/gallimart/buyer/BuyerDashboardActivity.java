package com.example.gallimart.buyer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gallimart.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BuyerDashboardActivity extends AppCompatActivity {

    private LocationFragment locationFragment;
    private InventoryFragment inventoryFragment;
    private CartFragment cartFragment;
    private ProfileFragment profileFragment;
    private BuyerOrdersFragment buyerOrderFragment;
    private Fragment activeFragment;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_dashboard);

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

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragmentToShow = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragmentToShow = locationFragment;
            } else if (id == R.id.nav_inventory) {
                if (inventoryFragment == null) {
                    inventoryFragment = new InventoryFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, inventoryFragment, "INVENTORY")
                            .hide(activeFragment)
                            .commit();
                    activeFragment = inventoryFragment;
                    return true; // handled
                } else {
                    fragmentToShow = inventoryFragment;
                }
            } else if (id == R.id.nav_cart) {
                fragmentToShow = cartFragment;
            } else if (id == R.id.nav_profile) {
                fragmentToShow = profileFragment;
            } else if (id == R.id.nav_orders) {
                fragmentToShow = buyerOrderFragment;
            }

            if (fragmentToShow != null && fragmentToShow != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(fragmentToShow)
                        .commit();
                activeFragment = fragmentToShow;
            }
            return true;
        });
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
    }

    public CartFragment getCartFragment() {
        return cartFragment;
    }
}
