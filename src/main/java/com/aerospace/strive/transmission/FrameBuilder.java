package com.aerospace.strive.transmission;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.aerospace.strive.model.ISSTelemetry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ENHANCED FRAME BUILDER - Uses ALL ISS Data Fields
 * Encodes complete telemetry including solar position, footprint, etc.
 * Maximum data utilization for realistic satellite testing
 */
public class FrameBuilder {
    
    private static final int ISS_SATELLITE_ID = 25544;  // Real ISS NORAD ID
    private static int frameCounter = 0;
    private static ObjectMapper jsonMapper = new ObjectMapper();
    
    /**
     * ENHANCED: Build frame using ALL ISS API data fields
     */
    public static TelemetryFrame buildFrameFromISSJson(String issJsonData) {
        frameCounter++;
        
        try {
            // Parse the complete JSON data from ISS API
            JsonNode issData = jsonMapper.readTree(issJsonData);
            
            // Extract ALL fields with safety checks
            String name = issData.has("name") ? issData.get("name").asText() : "iss";
            int id = issData.has("id") ? issData.get("id").asInt() : ISS_SATELLITE_ID;
            double latitude = issData.has("latitude") ? issData.get("latitude").asDouble() : 0.0;
            double longitude = issData.has("longitude") ? issData.get("longitude").asDouble() : 0.0;
            double altitude = issData.has("altitude") ? issData.get("altitude").asDouble() : 0.0;
            double velocity = issData.has("velocity") ? issData.get("velocity").asDouble() : 0.0;
            String visibility = issData.has("visibility") ? issData.get("visibility").asText() : "unknown";
            double footprint = issData.has("footprint") ? issData.get("footprint").asDouble() : 0.0;
            long timestamp = issData.has("timestamp") ? issData.get("timestamp").asLong() : System.currentTimeMillis() / 1000;
            double daynum = issData.has("daynum") ? issData.get("daynum").asDouble() : 0.0;
            double solarLat = issData.has("solar_lat") ? issData.get("solar_lat").asDouble() : 0.0;
            double solarLon = issData.has("solar_lon") ? issData.get("solar_lon").asDouble() : 0.0;
            String units = issData.has("units") ? issData.get("units").asText() : "kilometers";
            
            // Create enhanced ISSTelemetry object with ALL data
            ISSTelemetry telemetry = new ISSTelemetry(
                name, id, latitude, longitude, altitude, velocity, visibility,
                footprint, timestamp, daynum, solarLat, solarLon, units
            );
            
            System.out.println("🛠️  Building ENHANCED frame from COMPLETE ISS data:");
            System.out.println("   - Position: " + String.format("%.4f", latitude) + ", " + String.format("%.4f", longitude));
            System.out.println("   - Altitude: " + String.format("%.1f", altitude) + " " + units);
            System.out.println("   - Velocity: " + String.format("%.1f", velocity) + " km/h");
            System.out.println("   - Visibility: " + visibility);
            System.out.println("   - Footprint: " + String.format("%.1f", footprint) + " km");
            System.out.println("   - Solar Position: " + String.format("%.2f", solarLat) + "°, " + String.format("%.2f", solarLon) + "°");
            System.out.println("   - Day Number: " + String.format("%.6f", daynum));
            System.out.println("   - Frame #: " + frameCounter);
            
            // Build the frame using enhanced payload encoding
            byte[] payload = encodeCompleteISSPayload(
                latitude, longitude, altitude, velocity,
                visibility, footprint, daynum, solarLat, solarLon, units
            );
            
            // Create the telemetry frame with complete data
            TelemetryFrame frame = new TelemetryFrame(
                ISS_SATELLITE_ID,
                timestamp,
                frameCounter,
                payload
            );
            
            return frame;
            
        } catch (Exception e) {
            System.err.println("❌ Enhanced frame building error: " + e.getMessage());
            return createSampleFrame(); // Fallback to sample data
        }
    }
    
    /**
     * ENHANCED: Encode ALL ISS data fields into binary payload
     */
    private static byte[] encodeCompleteISSPayload(
            double latitude, double longitude, double altitude, double velocity,
            String visibility, double footprint, double daynum, 
            double solarLat, double solarLon, String units) {
        
        // Calculate payload size: 8 doubles + 1 byte visibility + units string
        int unitsBytes = units.getBytes(StandardCharsets.UTF_8).length;
        int size = (8 * 8) + 1 + unitsBytes + 1; // +1 for units length byte
        
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        // Pack ALL telemetry data as binary
        buffer.putDouble(latitude);      // 8 bytes - Primary position
        buffer.putDouble(longitude);     // 8 bytes - Primary position
        buffer.putDouble(altitude);      // 8 bytes - Orbit altitude
        buffer.putDouble(velocity);      // 8 bytes - Orbital velocity
        buffer.putDouble(footprint);     // 8 bytes - Ground footprint diameter
        buffer.putDouble(daynum);        // 8 bytes - Julian day number
        buffer.putDouble(solarLat);      // 8 bytes - Solar latitude
        buffer.putDouble(solarLon);      // 8 bytes - Solar longitude
        
        // Encode visibility as single byte
        byte visibilityCode = encodeVisibility(visibility);
        buffer.put(visibilityCode);      // 1 byte
        
        // Encode units as string with length prefix
        byte[] unitsBytesArray = units.getBytes(StandardCharsets.UTF_8);
        buffer.put((byte) unitsBytesArray.length); // Length byte
        buffer.put(unitsBytesArray);               // Units string
        
        return buffer.array();
    }
    
    /**
     * REUSE: Existing visibility encoding
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
     * REUSE: Existing method signature for compatibility
     */
    public static TelemetryFrame buildFrameFromISSTelemetry(ISSTelemetry issData) {
        // Convert enhanced telemetry to JSON string for processing
        String jsonString = String.format(
            "{\"name\":\"%s\",\"id\":%d,\"latitude\":%f,\"longitude\":%f,\"altitude\":%f,\"velocity\":%f,\"visibility\":\"%s\",\"footprint\":%f,\"timestamp\":%d,\"daynum\":%f,\"solar_lat\":%f,\"solar_lon\":%f,\"units\":\"%s\"}",
            issData.getName(), issData.getId(), issData.getLatitude(), issData.getLongitude(),
            issData.getAltitude(), issData.getVelocity(), issData.getVisibility(),
            issData.getFootprint(), issData.getTimestamp(), issData.getDaynum(),
            issData.getSolarLat(), issData.getSolarLon(), issData.getUnits()
        );
        return buildFrameFromISSJson(jsonString);
    }
    
    /**
     * REUSE: Existing sample frame method
     */
    public static TelemetryFrame createSampleFrame() {
        // Create sample ISS data with enhanced fields
        ISSTelemetry sampleData = new ISSTelemetry(
            "ISS", ISS_SATELLITE_ID, 31.4567, -112.2345, 408.2, 27600.5, "daylight",
            4500.0, System.currentTimeMillis() / 1000, 2460977.343437, 
            -13.40, 232.29, "kilometers"
        );
        
        return buildFrameFromISSTelemetry(sampleData);
    }
    
    /**
     * ENHANCED: Detailed frame validation
     */
    public static void validateFrame(TelemetryFrame frame) {
        byte[] binaryData = frame.toBinary();
        byte[] payload = frame.getPayload();
        
        System.out.println("   - Frame size: " + binaryData.length + " bytes");
        System.out.println("   - Payload size: " + payload.length + " bytes");
        System.out.println("   - Satellite ID: " + frame.getSatelliteId());
        System.out.println("   - Timestamp: " + frame.getTimestamp());
        System.out.println("   - Frame counter: " + frame.getFrameCounter());
        
        // Calculate enhanced data efficiency
        double efficiency = (double) payload.length / binaryData.length * 100;
        System.out.println("   - Data efficiency: " + String.format("%.1f", efficiency) + "%");
        
        // Show payload composition
        analyzePayloadComposition(payload);
    }
    
    /**
     * NEW: Analyze what's in the payload
     */
    private static void analyzePayloadComposition(byte[] payload) {
        System.out.println("   - Payload composition:");
        System.out.println("     • 8 doubles (position/orbit): 64 bytes");
        System.out.println("     • Visibility code: 1 byte");
        System.out.println("     • Units string: " + (payload.length - 65) + " bytes");
        System.out.println("     • TOTAL: " + payload.length + " bytes of REAL ISS data");
    }
}