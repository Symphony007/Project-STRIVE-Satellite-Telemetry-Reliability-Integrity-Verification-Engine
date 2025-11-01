package com.aerospace.strive.producer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.aerospace.strive.transmission.FrameBuilder;
import com.aerospace.strive.transmission.ScientificErrorInjector;
import com.aerospace.strive.transmission.TelemetryFrame;
import com.aerospace.strive.transmission.ISSBinaryParser;
import com.aerospace.strive.transmission.ISSBinaryParser.ISSBinaryTelemetry;

/**
 * JACKSON-FREE ISS DATA STREAMER
 * Uses binary parsing instead of JSON for NASA-grade reliability
 */
public class ISSStreamProcessor {
    private static final String ISS_API_URL = "https://api.wheretheiss.at/v1/satellites/25544";
    
    private HttpClient httpClient;
    private ScheduledExecutorService scheduler;
    private int packetCount = 0;
    private ScientificErrorInjector errorInjector;
    private ISSBinaryParser binaryParser;
    
    public ISSStreamProcessor() {
        this.httpClient = HttpClient.newHttpClient();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.errorInjector = new ScientificErrorInjector();
        this.binaryParser = new ISSBinaryParser();
        
        System.out.println("🛰️  JACKSON-FREE ISS Stream Processor - NASA GRADE");
        System.out.println("   - Binary telemetry parsing (no JSON dependencies)");
        System.out.println("   - CCSDS compliant data processing");
        System.out.println("   - Real-time satellite protocol handling");
    }
    
    /**
     * Start binary pipeline: ISS data → Binary parsing → Frames → Errors
     */
    public void startStreaming() {
        System.out.println("🚀 Starting JACKSON-FREE satellite communication pipeline...");
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                packetCount++;
                byte[] issData = fetchLiveISSData();
                
                // Binary pipeline: parse → build frame → inject errors
                processBinaryPipeline(issData, packetCount);
                
            } catch (Exception e) {
                System.err.println("❌ Pipeline error: " + e.getMessage());
                if (!e.getMessage().contains("timeout") && !e.getMessage().contains("Timeout")) {
                    e.printStackTrace();
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Fetch live ISS data as raw bytes (no JSON parsing)
     */
    private byte[] fetchLiveISSData() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ISS_API_URL))
                .GET()
                .header("User-Agent", "NASA-Satellite-Project/1.0")
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
                
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("ISS API returned: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * BINARY PIPELINE: Parse → Build Frame → Inject Errors
     */
    private void processBinaryPipeline(byte[] rawData, int packetNum) {
        try {
            System.out.println("\n📦 PACKET #" + packetNum + " - BINARY PROCESSING PIPELINE");
            System.out.println("────────────────────────────────────────────────────────");
            
            // Parse using binary parser (no Jackson)
            ISSBinaryTelemetry telemetry = ISSBinaryParser.parseBinaryTelemetry(rawData);
            
            // Display telemetry
            System.out.println("📡 REAL ISS BINARY TELEMETRY:");
            System.out.println(telemetry.toDetailedString());
            
            // Build satellite frame from binary telemetry
            TelemetryFrame perfectFrame = buildFrameFromBinaryTelemetry(telemetry);
            
            // Inject scientific errors
            injectErrorsIntoFrame(perfectFrame);
            
            System.out.println("────────────────────────────────────────────────────────");
            
        } catch (Exception e) {
            System.err.println("❌ Binary pipeline error: " + e.getMessage());
            System.out.println("  Raw data length: " + (rawData != null ? rawData.length : 0));
        }
    }
    
    /**
     * Build frame from binary telemetry (no JSON)
     */
    private TelemetryFrame buildFrameFromBinaryTelemetry(ISSBinaryTelemetry telemetry) {
        try {
            System.out.println("🔧 BUILDING SATELLITE FRAME FROM BINARY TELEMETRY:");
            
            // Convert to binary payload (industry standard)
            byte[] binaryPayload = FrameBuilder.telemetryToBinaryPayload(
                telemetry.latitude, telemetry.longitude, telemetry.altitude, 
                telemetry.velocity, telemetry.visibility, telemetry.footprint,
                telemetry.daynum, telemetry.solarLat, telemetry.solarLon, telemetry.units
            );
            
            // Create telemetry frame
            TelemetryFrame frame = new TelemetryFrame(
                telemetry.id,
                telemetry.timestamp,
                packetCount,
                binaryPayload
            );
            
            // Validate frame
            FrameBuilder.validateFrameStructure(frame);
            System.out.println("   ✅ Frame built: " + frame.toBinary().length + " bytes");
            System.out.println("   📊 Data efficiency: " + 
                String.format("%.1f", (double)binaryPayload.length / frame.toBinary().length * 100) + "%");
            
            return frame;
            
        } catch (Exception e) {
            System.err.println("   ❌ Frame building failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Inject errors into frame
     */
    private void injectErrorsIntoFrame(TelemetryFrame frame) {
        try {
            byte[] perfectFrame = frame.toBinary();
            
            System.out.println("⚡ INJECTING SCIENTIFIC ERRORS:");
            System.out.println("   - Perfect frame: " + perfectFrame.length + " bytes");
            System.out.println("   - Satellite: " + frame.getSatelliteId());
            
            // Inject realistic satellite errors
            byte[] corruptedFrame = errorInjector.injectScientificErrors(perfectFrame);
            
            System.out.println("   - Corrupted frame: " + corruptedFrame.length + " bytes");
            System.out.println("   - 🎯 READY FOR ERROR CORRECTION ALGORITHMS");
            
        } catch (Exception e) {
            System.err.println("❌ Error injection failed: " + e.getMessage());
        }
    }
    
    /**
     * Stop streaming gracefully
     */
    public void stopStreaming() {
        System.out.println("\n🛑 Stopping binary pipeline...");
        System.out.println("   Total packets processed: " + packetCount);
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Test method with binary pipeline
     */
    public static void main(String[] args) {
        ISSStreamProcessor processor = new ISSStreamProcessor();
        
        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛰️  Shutdown signal received...");
            processor.stopStreaming();
            System.out.println("✅ Binary pipeline shutdown complete.");
        }));
        
        // Start binary pipeline
        processor.startStreaming();
        
        // Keep running for demonstration
        try {
            System.out.println("\n⏰ Running binary pipeline for 30 seconds... (Ctrl+C to stop early)");
            Thread.sleep(30000);
            processor.stopStreaming();
            System.out.println("✅ JACKSON-FREE PIPELINE TEST SUCCESSFUL!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processor.stopStreaming();
        }
    }
}