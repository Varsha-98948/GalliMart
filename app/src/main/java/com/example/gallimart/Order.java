package com.example.gallimart;

import com.example.gallimart.SessionManager;
import java.util.List;

public class Order {
    public String orderId;
    public String buyerId;
    public String buyerName;
    public String shopId;
    public List<SessionManager.CartItem> items;
    public double totalAmount;
    public String status; // CONFIRMED, AWAITING_ACCEPTANCE, ACCEPTED, ASSIGNED_TO_DRIVER
    public long timestamp;

    public Order() {
        // Needed for Firebase
    }

    public Order(String orderId, String buyerId, String buyerName, String shopId,
                 List<SessionManager.CartItem> items, double totalAmount,
                 String status, long timestamp) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.shopId = shopId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.timestamp = timestamp;
    }
}
