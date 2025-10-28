package com.aerospace.strive.model;

/**
 * ENHANCED ISS Telemetry Model - Contains ALL Real ISS Data Fields
 * Complete representation of ISS API response for maximum data utilization
 */
public class ISSTelemetry {
    private final String name;
    private final int id;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final double velocity;
    private final String visibility;
    private final double footprint;
    private final long timestamp;
    private final double daynum;
    private final double solarLat;
    private final double solarLon;
    private final String units;
    
    // Original constructor for backward compatibility
    public ISSTelemetry(String name, int id, double latitude, double longitude, 
                       double altitude, double velocity, String visibility, long timestamp) {
        this(name, id, latitude, longitude, altitude, velocity, visibility, 
             0.0, timestamp, 0.0, 0.0, 0.0, "kilometers");
    }
    
    // ENHANCED constructor with ALL fields
    public ISSTelemetry(String name, int id, double latitude, double longitude, 
                       double altitude, double velocity, String visibility,
                       double footprint, long timestamp, double daynum,
                       double solarLat, double solarLon, String units) {
        this.name = name;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.velocity = velocity;
        this.visibility = visibility;
        this.footprint = footprint;
        this.timestamp = timestamp;
        this.daynum = daynum;
        this.solarLat = solarLat;
        this.solarLon = solarLon;
        this.units = units;
    }
    
    // Getters for ALL fields
    public String getName() { return name; }
    public int getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
    public double getVelocity() { return velocity; }
    public String getVisibility() { return visibility; }
    public double getFootprint() { return footprint; }
    public long getTimestamp() { return timestamp; }
    public double getDaynum() { return daynum; }
    public double getSolarLat() { return solarLat; }
    public double getSolarLon() { return solarLon; }
    public String getUnits() { return units; }
    
    @Override
    public String toString() {
        return String.format("ISS[lat=%.2f, lon=%.2f, alt=%.1f%s, vel=%.1fkm/h, vis=%s, foot=%.1fkm]", 
                           latitude, longitude, altitude, units, velocity, visibility, footprint);
    }
    
    // Enhanced toString with all fields
    public String toStringDetailed() {
        return String.format(
            "ISS Detailed:\n" +
            "  Name: %s (ID: %d)\n" +
            "  Position: %.4f° lat, %.4f° lon\n" +
            "  Orbit: %.1f %s altitude, %.1f km/h velocity\n" +
            "  Visibility: %s, Footprint: %.1f km\n" +
            "  Time: %d (Unix), Day: %.6f\n" +
            "  Solar: %.2f° lat, %.2f° lon",
            name, id, latitude, longitude, altitude, units, velocity,
            visibility, footprint, timestamp, daynum, solarLat, solarLon
        );
    }
}