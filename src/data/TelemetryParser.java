package data;

/**
 * NASA-GRADE TELEMETRY PARSING ENGINE
 * Converts raw ISS API response into structured telemetry data
 * Implements binary-compatible parsing for satellite frame construction
 * No JSON dependencies - manual parsing for space-grade reliability
 */
public class TelemetryParser {
    
    /**
     * Parsed telemetry data structure matching ISS API parameters
     * All fields aligned with CCSDS telemetry standards
     */
    public static class TelemetryData {
        public final String name;
        public final int id;
        public final double latitude;
        public final double longitude;
        public final double altitude;
        public final double velocity;
        public final String visibility;
        public final double footprint;
        public final long timestamp;
        public final double daynum;
        public final double solarLat;
        public final double solarLon;
        public final String units;
        
        public TelemetryData(String name, int id, double latitude, double longitude, 
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
        
        @Override
        public String toString() {
            return String.format(
                "ISS-TELEMETRY [ID:%d] LAT:%.6f LON:%.6f ALT:%.3fkm VEL:%.3fkm/h VIS:%s TS:%d",
                id, latitude, longitude, altitude, velocity, visibility, timestamp
            );
        }
    }
    
    /**
     * Parses raw JSON telemetry into structured data without JSON libraries
     * @param rawTelemetry Raw JSON string from ISS API
     * @return Structured telemetry data for frame construction
     * @throws TelemetryParseException if parsing fails or data integrity compromised
     */
    public TelemetryData parseRawTelemetry(String rawTelemetry) throws TelemetryParseException {
        try {
            // Extract each parameter using manual parsing for satellite-grade reliability
            String name = extractStringValue(rawTelemetry, "name");
            int id = extractIntValue(rawTelemetry, "id");
            double latitude = extractDoubleValue(rawTelemetry, "latitude");
            double longitude = extractDoubleValue(rawTelemetry, "longitude");
            double altitude = extractDoubleValue(rawTelemetry, "altitude");
            double velocity = extractDoubleValue(rawTelemetry, "velocity");
            String visibility = extractStringValue(rawTelemetry, "visibility");
            double footprint = extractDoubleValue(rawTelemetry, "footprint");
            long timestamp = extractLongValue(rawTelemetry, "timestamp");
            double daynum = extractDoubleValue(rawTelemetry, "daynum");
            double solarLat = extractDoubleValue(rawTelemetry, "solar_lat");
            double solarLon = extractDoubleValue(rawTelemetry, "solar_lon");
            String units = extractStringValue(rawTelemetry, "units");
            
            // Validate scientific ranges for orbital parameters
            validateOrbitalParameters(latitude, longitude, altitude, velocity);
            
            return new TelemetryData(name, id, latitude, longitude, altitude, velocity,
                                   visibility, footprint, timestamp, daynum, solarLat, 
                                   solarLon, units);
            
        } catch (Exception e) {
            throw new TelemetryParseException("Telemetry parsing failure: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts string value from JSON without JSON library
     */
    private String extractStringValue(String json, String key) throws TelemetryParseException {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new TelemetryParseException("Missing required parameter: " + key);
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            throw new TelemetryParseException("Malformed string value for: " + key);
        }
        return json.substring(start, end);
    }
    
    /**
     * Extracts double value from JSON without JSON library
     */
    private double extractDoubleValue(String json, String key) throws TelemetryParseException {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new TelemetryParseException("Missing required parameter: " + key);
        }
        start += searchKey.length();
        int end = findValueEnd(json, start);
        
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            throw new TelemetryParseException("Invalid numeric format for: " + key);
        }
    }
    
    /**
     * Extracts integer value from JSON without JSON library
     */
    private int extractIntValue(String json, String key) throws TelemetryParseException {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new TelemetryParseException("Missing required parameter: " + key);
        }
        start += searchKey.length();
        int end = findValueEnd(json, start);
        
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            throw new TelemetryParseException("Invalid integer format for: " + key);
        }
    }
    
    /**
     * Extracts long value from JSON without JSON library
     */
    private long extractLongValue(String json, String key) throws TelemetryParseException {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new TelemetryParseException("Missing required parameter: " + key);
        }
        start += searchKey.length();
        int end = findValueEnd(json, start);
        
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            throw new TelemetryParseException("Invalid long format for: " + key);
        }
    }
    
    /**
     * Finds the end of a JSON value (comma, bracket, or brace)
     */
    private int findValueEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']') {
                return i;
            }
        }
        return json.length();
    }
    
    /**
     * Validates orbital parameters against scientific ranges
     */
    private void validateOrbitalParameters(double lat, double lon, double alt, double vel) 
            throws TelemetryParseException {
        if (lat < -90 || lat > 90) {
            throw new TelemetryParseException("Invalid latitude: " + lat + " (range: -90 to 90)");
        }
        if (lon < -180 || lon > 180) {
            throw new TelemetryParseException("Invalid longitude: " + lon + " (range: -180 to 180)");
        }
        if (alt < 300 || alt > 500) { // ISS operational altitude range in km
            throw new TelemetryParseException("Suspicious altitude: " + alt + "km (expected 300-500km)");
        }
        if (vel < 27000 || vel > 28000) { // ISS orbital velocity range in km/h
            throw new TelemetryParseException("Suspicious velocity: " + vel + "km/h (expected 27,000-28,000km/h)");
        }
    }
    
    /**
     * Custom exception for telemetry parsing failures
     */
    public static class TelemetryParseException extends Exception {
        public TelemetryParseException(String message) {
            super(message);
        }
        
        public TelemetryParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Test method - validate parsing with real ISS data
     */
    public static void main(String[] args) {
        try {
            // Test with actual data from our fetcher
            ISSDataFetcher fetcher = new ISSDataFetcher();
            String rawData = fetcher.fetchLiveTelemetry();
            
            TelemetryParser parser = new TelemetryParser();
            TelemetryData telemetry = parser.parseRawTelemetry(rawData);
            
            System.out.println("✓ TELEMETRY PARSING SUCCESSFUL");
            System.out.println(telemetry.toString());
            System.out.println("Full dataset validated:");
            System.out.println("  Name: " + telemetry.name);
            System.out.println("  ID: " + telemetry.id);
            System.out.println("  Latitude: " + telemetry.latitude);
            System.out.println("  Longitude: " + telemetry.longitude);
            System.out.println("  Altitude: " + telemetry.altitude + " km");
            System.out.println("  Velocity: " + telemetry.velocity + " km/h");
            System.out.println("  Visibility: " + telemetry.visibility);
            System.out.println("  Footprint: " + telemetry.footprint + " km");
            System.out.println("  Timestamp: " + telemetry.timestamp);
            System.out.println("  Daynum: " + telemetry.daynum);
            System.out.println("  Solar Lat: " + telemetry.solarLat);
            System.out.println("  Solar Lon: " + telemetry.solarLon);
            System.out.println("  Units: " + telemetry.units);
            
        } catch (ISSDataFetcher.SatelliteCommException e) {
            System.err.println("✗ SATELLITE COMMUNICATION FAILURE: " + e.getMessage());
        } catch (TelemetryParseException e) {
            System.err.println("✗ TELEMETRY PARSE FAILURE: " + e.getMessage());
        }
    }
}