package com.aerospace.strive.transmission;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * JACKSON-FREE FRAME BUILDER
 * Uses binary data processing instead of JSON
 */
public class FrameBuilder {
    
    private static final int ISS_SATELLITE_ID = 25544;
    private static int frameCounter = 0;
    
    /**
     * Build frame from binary telemetry data (no JSON)
     */
    public static TelemetryFrame buildFrameFromBinaryTelemetry(double latitude, double longitude, 
                                                             double altitude, double velocity,
                                                             String visibility, double footprint,
                                                             double daynum, double solarLat, 
                                                             double solarLon, String units) {
        frameCounter++;
        
        System.out.println("🛰️  BUILDING FRAME FROM BINARY TELEMETRY #" + frameCounter);
        System.out.println("   Position: " + String.format("%.4f", latitude) + ", " + String.format("%.4f", longitude));
        System.out.println("   Altitude: " + String.format("%.1f", altitude) + " " + units);
        System.out.println("   Velocity: " + String.format("%.1f", velocity) + " km/h");
        
        // Build binary payload
        byte[] payload = telemetryToBinaryPayload(latitude, longitude, altitude, velocity,
                                                 visibility, footprint, daynum, solarLat, solarLon, units);
        
        // Create frame
        TelemetryFrame frame = new TelemetryFrame(
            ISS_SATELLITE_ID,
            System.currentTimeMillis() / 1000,
            frameCounter,
            payload
        );
        
        return frame;
    }
    
    /**
     * Convert telemetry to binary payload (industry standard)
     */
    public static byte[] telemetryToBinaryPayload(double latitude, double longitude, double altitude,
                                                 double velocity, String visibility, double footprint,
                                                 double daynum, double solarLat, double solarLon, 
                                                 String units) {
        // Calculate payload size for efficiency
        int unitsBytes = units.getBytes(StandardCharsets.UTF_8).length;
        int visibilityBytes = visibility.getBytes(StandardCharsets.UTF_8).length;
        int size = (8 * 8) + 1 + visibilityBytes + 1 + unitsBytes;
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        // Pack all telemetry as binary (industry standard)
        buffer.putDouble(latitude);
        buffer.putDouble(longitude); 
        buffer.putDouble(altitude);
        buffer.putDouble(velocity);
        buffer.putDouble(footprint);
        buffer.putDouble(daynum);
        buffer.putDouble(solarLat);
        buffer.putDouble(solarLon);
        
        // Encode strings with length prefixes
        buffer.put((byte) visibilityBytes);
        buffer.put(visibility.getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) unitsBytes);
        buffer.put(units.getBytes(StandardCharsets.UTF_8));
        
        return buffer.array();
    }
    
    /**
     * Create sample frame without JSON
     */
    public static TelemetryFrame createSampleFrame() {
        return buildFrameFromBinaryTelemetry(
            31.4567, -112.2345, 408.2, 27600.5, "daylight",
            4500.0, 2460977.343437, -13.40, 232.29, "kilometers"
        );
    }
    
    /**
     * Validate frame structure
     */
    public static void validateFrameStructure(TelemetryFrame frame) {
        byte[] binaryData = frame.toBinary();
        byte[] payload = frame.getPayload();
        
        System.out.println("   ✅ Frame validated:");
        System.out.println("     - Total size: " + binaryData.length + " bytes");
        System.out.println("     - Payload: " + payload.length + " bytes");
        System.out.println("     - Satellite: " + frame.getSatelliteId());
    }
}