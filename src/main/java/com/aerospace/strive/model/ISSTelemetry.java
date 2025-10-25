package com.aerospace.strive.model;

/**
 * Represents real ISS telemetry data from the WhereTheISS.at API
 */
public class ISSTelemetry {
    private final String name;
    private final int id;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final double velocity;
    private final String visibility;
    private final long timestamp;
    
    public ISSTelemetry(String name, int id, double latitude, double longitude, 
                       double altitude, double velocity, String visibility, long timestamp) {
        this.name = name;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.velocity = velocity;
        this.visibility = visibility;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getName() { return name; }
    public int getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
    public double getVelocity() { return velocity; }
    public String getVisibility() { return visibility; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("ISS[lat=%.2f, lon=%.2f, alt=%.1fkm, vel=%.1fkm/h]", 
                           latitude, longitude, altitude, velocity);
    }
}