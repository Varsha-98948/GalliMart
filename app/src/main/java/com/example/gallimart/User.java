package com.example.gallimart;

public class User {
    public String uid;
    public String name;
    public String email;
    public String phone;
    public String role; // Buyer / Shopkeeper / Driver
    public long createdAt;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String uid, String name, String email, String phone, String role, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
    }
}
