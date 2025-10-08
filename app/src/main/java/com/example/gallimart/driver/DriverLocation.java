package com.example.gallimart.driver;

public class DriverLocation {
    private String pickup;
    private String delivery;

    public DriverLocation(String pickup, String delivery) {
        this.pickup = pickup;
        this.delivery = delivery;
    }

    public String getPickup() { return pickup; }
    public String getDelivery() { return delivery; }
}