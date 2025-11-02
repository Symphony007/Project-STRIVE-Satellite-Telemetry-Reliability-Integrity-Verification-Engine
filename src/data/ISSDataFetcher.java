package data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * NASA-GRADE ISS TELEMETRY ACQUISITION
 * Fetches real-time ISS positional data from wheretheiss.at API
 * Returns raw telemetry string for binary parsing
 * Implements space-grade error handling and connection resilience
 */
public class ISSDataFetcher {
    
    private static final String ISS_API_URL = "https://api.wheretheiss.at/v1/satellites/25544";
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds - satellite comms standard
    private static final int READ_TIMEOUT = 10000; // 10 seconds - orbital position latency
    
    // Telemetry parameters mirroring actual ISS data structure
    private static final String[] REQUIRED_PARAMS = {
        "latitude", "longitude", "altitude", "velocity", 
        "visibility", "footprint", "timestamp", "daynum",
        "solar_lat", "solar_lon", "units"
    };
    
    /**
     * Fetches current ISS telemetry from live API
     * @return Raw telemetry string in API response format
     * @throws SatelliteCommException if communication fails beyond operational limits
     */
    public String fetchLiveTelemetry() throws SatelliteCommException {
        HttpURLConnection connection = null;
        int retryCount = 0;
        final int MAX_RETRIES = 3; // Standard satellite communication retry protocol
        
        while (retryCount < MAX_RETRIES) {
            try {
                URL url = new URL(ISS_API_URL);
                connection = (HttpURLConnection) url.openConnection();
                
                // Configure for satellite communication standards
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestProperty("User-Agent", "NASA-STRIVE-Satellite-System/1.0");
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    String rawTelemetry = response.toString();
                    validateTelemetryCompleteness(rawTelemetry);
                    
                    return rawTelemetry;
                    
                } else if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                    // Server maintenance or overload - standard satellite operation response
                    retryCount++;
                    if (retryCount == MAX_RETRIES) {
                        throw new SatelliteCommException(
                            "ISS API service unavailable after " + MAX_RETRIES + " attempts. " +
                            "Orbital station maintenance in progress.");
                    }
                    Thread.sleep(1000); // Exponential backoff simplified
                } else {
                    throw new SatelliteCommException("HTTP error code: " + responseCode + 
                                                    " - Satellite ground station communication failure");
                }
                
            } catch (java.net.SocketTimeoutException e) {
                retryCount++;
                if (retryCount == MAX_RETRIES) {
                    throw new SatelliteCommException(
                        "Orbital communication timeout after " + MAX_RETRIES + " attempts. " +
                        "ISS may be in orbital blind spot.", e);
                }
            } catch (java.net.UnknownHostException e) {
                throw new SatelliteCommException(
                    "Ground station resolution failure. Network configuration error.", e);
            } catch (java.io.IOException e) {
                retryCount++;
                if (retryCount == MAX_RETRIES) {
                    throw new SatelliteCommException(
                        "I/O operation failure during satellite communication.", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SatelliteCommException("Satellite communication thread interrupted", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        throw new SatelliteCommException("Maximum communication retries exceeded");
    }
    
    /**
     * Validates telemetry contains all required parameters for satellite operations
     * @param rawTelemetry Raw JSON string from API
     * @throws SatelliteCommException if critical telemetry parameters are missing
     */
    private void validateTelemetryCompleteness(String rawTelemetry) throws SatelliteCommException {
        for (String param : REQUIRED_PARAMS) {
            if (!rawTelemetry.contains("\"" + param + "\":")) {
                throw new SatelliteCommException(
                    "Incomplete telemetry: missing parameter '" + param + "'. " +
                    "Data integrity compromised for satellite operations.");
            }
        }
    }
    
    /**
     * Custom exception for satellite communication failures
     * Mirrors NASA ground station error reporting standards
     */
    public static class SatelliteCommException extends Exception {
        public SatelliteCommException(String message) {
            super(message);
        }
        
        public SatelliteCommException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Test method - validate real data acquisition
     */
    public static void main(String[] args) {
        ISSDataFetcher fetcher = new ISSDataFetcher();
        try {
            String telemetry = fetcher.fetchLiveTelemetry();
            System.out.println("✓ REAL ISS TELEMETRY ACQUISITION SUCCESSFUL");
            System.out.println("Raw data length: " + telemetry.length() + " bytes");
            System.out.println("First 200 chars: " + telemetry.substring(0, Math.min(200, telemetry.length())));
        } catch (SatelliteCommException e) {
            System.err.println("✗ SATELLITE COMMUNICATION FAILURE: " + e.getMessage());
        }
    }
}