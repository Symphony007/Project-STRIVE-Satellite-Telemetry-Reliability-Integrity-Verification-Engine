package com.aerospace.strive.transmission;

import java.nio.ByteBuffer;

import com.aerospace.strive.model.ISSTelemetry;

/**
 * Converts real ISS telemetry data into binary satellite frames
 */
public class FrameBuilder {
    
    private static final int ISS_SATELLITE_ID = 25544;  // Real ISS NORAD ID
    private static int frameCounter = 0;
    
    /**
     * Converts ISS API data into a binary telemetry frame
     */
    public static TelemetryFrame buildFrameFromISSTelemetry(ISSTelemetry issData) {
        frameCounter++;
        
        // Convert ISS data to binary payload
        byte[] payload = encodeISSPayload(issData);
        
        // Create the telemetry frame
        return new TelemetryFrame(
            ISS_SATELLITE_ID,
            issData.getTimestamp(),
            frameCounter,
            payload
        );
    }
    
    /**
     * Encodes ISS telemetry into binary format for transmission
     * This simulates how real satellites pack sensor data
     */
    private static byte[] encodeISSPayload(ISSTelemetry issData) {
        // Calculate payload size: 4 doubles (lat, lon, alt, vel) + 1 byte for visibility
        int size = (4 * 8) + 1; // 4 doubles (8 bytes each) + 1 byte for visibility
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        // Pack telemetry data as binary (like real satellites do)
        buffer.putDouble(issData.getLatitude());    // 8 bytes
        buffer.putDouble(issData.getLongitude());   // 8 bytes  
        buffer.putDouble(issData.getAltitude());    // 8 bytes
        buffer.putDouble(issData.getVelocity());    // 8 bytes
        
        // Encode visibility as single byte (0=daylight, 1=night, 2=eclipse)
        byte visibilityCode = encodeVisibility(issData.getVisibility());
        buffer.put(visibilityCode);                 // 1 byte
        
        return buffer.array();
    }
    
    /**
     * Converts visibility string to compact binary code
     */
    private static byte encodeVisibility(String visibility) {
        if ("daylight".equalsIgnoreCase(visibility)) {
            return 0;
        } else if ("night".equalsIgnoreCase(visibility)) {
            return 1;
        } else if ("eclipse".equalsIgnoreCase(visibility)) {
            return 2;
        } else {
            return 3; // unknown
        }
    }
    
    /**
     * For testing - creates a sample frame from current ISS data
     */
    public static TelemetryFrame createSampleFrame() {
        // Create sample ISS data (you'll replace this with real API data)
        ISSTelemetry sampleData = new ISSTelemetry(
            "ISS",
            ISS_SATELLITE_ID,
            31.4567,    // latitude
            -112.2345,  // longitude  
            408.2,      // altitude
            27600.5,    // velocity
            "daylight", // visibility
            System.currentTimeMillis() / 1000  // current Unix time
        );
        
        return buildFrameFromISSTelemetry(sampleData);
    }
}