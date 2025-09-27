package com.example.gallimart;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SessionManager {

    private static final String PREF_NAME = "GalliMartSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_SHOP_ID = "shopId";
    private static final String KEY_CART = "cartItems";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    // Listener for cart changes
    public interface CartChangeListener {
        void onCartChanged();
    }

    // *** KEY CHANGE: shared listeners across all SessionManager instances ***
    private static final List<CartChangeListener> cartListeners = new CopyOnWriteArrayList<>();

    public void addCartChangeListener(CartChangeListener listener) {
        if (listener != null && !cartListeners.contains(listener)) {
            cartListeners.add(listener);
        }
    }

    public void removeCartChangeListener(CartChangeListener listener) {
        cartListeners.remove(listener);
    }

    private void notifyCartChanged() {
        for (CartChangeListener l : cartListeners) {
            try {
                l.onCartChanged();
            } catch (Throwable t) {
                // swallow to avoid one faulty listener breaking others
            }
        }
    }

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    // User Session
    public void createLoginSession(String name, String email, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logout(boolean clearCart) {
        if (clearCart) editor.clear();
        else {
            editor.remove(KEY_IS_LOGGED_IN);
            editor.remove(KEY_USER_NAME);
            editor.remove(KEY_USER_EMAIL);
            editor.remove(KEY_USER_ROLE);
            editor.remove(KEY_SHOP_ID);
        }
        editor.apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "User");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "user@example.com");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "Buyer");
    }

    // Shop ID
    public void setShopId(String shopId) {
        editor.putString(KEY_SHOP_ID, shopId).apply();
    }

    public String getShopId() {
        return prefs.getString(KEY_SHOP_ID, null);
    }

    // CartItem Model
    public static class CartItem {
        public String id;
        public String name;
        public double price;
        public String imageUrl;
        public int quantity;

        public CartItem() {}

        public CartItem(String id, String name, double price, String imageUrl, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.quantity = quantity;
        }
    }

    // Cart Management
    public void saveCart(Map<String, CartItem> cart) {
        editor.putString(KEY_CART, gson.toJson(cart));
        editor.apply();
        notifyCartChanged();
    }

    public Map<String, CartItem> getCart() {
        String json = prefs.getString(KEY_CART, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void clearCart() {
        editor.remove(KEY_CART).apply();
        notifyCartChanged();
    }

    public int getCartItemCount() {
        int count = 0;
        for (CartItem item : getCart().values()) count += item.quantity;
        return count;
    }

    public void addItemToCart(String id, String name, double price, String imageUrl) {
        Map<String, CartItem> cart = getCart();
        CartItem existing = cart.get(id);
        if (existing != null) {
            existing.quantity++;
            cart.put(id, existing);
        } else {
            existing = new CartItem(id, name, price, imageUrl, 1);
            cart.put(id, existing);
        }
        saveCart(cart); // triggers listeners
    }

    public void removeItemFromCart(String id) {
        Map<String, CartItem> cart = getCart();
        if (cart.containsKey(id)) {
            CartItem existing = cart.get(id);
            if (existing.quantity > 1) {
                existing.quantity--;
                cart.put(id, existing);
            } else {
                cart.remove(id);
            }
            saveCart(cart); // triggers listeners
        }
    }
}
