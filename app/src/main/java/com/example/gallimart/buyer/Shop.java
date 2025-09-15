package com.example.gallimart.buyer;

public class Shop {
    private String id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Float distance; // Add this field for distance in km

    public Shop() {} // Required for Firebase

    public Shop(String id, String name, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Float getDistance() { return distance; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setDistance(Float distance) { this.distance = distance; }
}
