package com.example.gallimart.buyer;

public class Shop {
    private String shopId;
    private String name;
    private String email;
    private double lat = 0;
    private double lng = 0;
    private double distance;

    public Shop() {} // empty for Firebase

    public Shop(String shopId, String name, String email, double lat, double lng) {
        this.shopId = shopId;
        this.name = name;
        this.email = email;
        this.lat = lat;
        this.lng = lng;
    }

    public String getShopId() { return shopId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public double getDistance() { return distance; }

    public void setId(String id) { this.shopId = id; }
    public void setName(String name) { this.name = name; }
    public void setLatitude(Double latitude) { this.lat = latitude; }
    public void setLongitude(Double longitude) { this.lng = longitude; }
    public void setDistance(Double distance) { this.distance = distance; }
}
