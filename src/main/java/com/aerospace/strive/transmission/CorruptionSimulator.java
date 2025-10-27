package com.aerospace.strive.transmission;

import java.util.Arrays;
import java.util.Random;

/**
 * Simulates realistic satellite transmission errors with STRUCTURAL IMPACT
 * Based on actual RF impairments in space communications
 * ENHANCED VERSION - targets frame structure specifically
 */
public class CorruptionSimulator {
    
    private static final Random random = new Random();
    
    // Error type probabilities (configurable)
    public static final double BURST_NOISE_PROB = 0.10;      // 10%
    public static final double HEADER_DRIFT_PROB = 0.05;     // 5%  
    public static final double PACKET_TRUNCATION_PROB = 0.08; // 8%
    public static final double BIT_FLIP_PROB = 0.15;         // 15%
    public static final double RANDOM_NOISE_PROB = 0.07;     // 7%
    
    /**
     * Applies realistic satellite transmission errors to a frame
     * ENHANCED: Targets critical frame structures specifically
     */
    public static byte[] applyTransmissionErrors(byte[] originalFrame) {
        byte[] corrupted = Arrays.copyOf(originalFrame, originalFrame.length);
        
        // Apply different error types based on probability
        if (random.nextDouble() < BURST_NOISE_PROB) {
            corrupted = injectBurstNoise(corrupted);
        }
        
        if (random.nextDouble() < HEADER_DRIFT_PROB) {
            corrupted = injectHeaderDrift(corrupted);
        }
        
        if (random.nextDouble() < PACKET_TRUNCATION_PROB) {
            corrupted = injectPacketTruncation(corrupted);
        }
        
        if (random.nextDouble() < BIT_FLIP_PROB) {
            // Target header specifically for structural impact
            corrupted = injectTargetedBitFlips(corrupted);
        }
        
        if (random.nextDouble() < RANDOM_NOISE_PROB) {
            corrupted = injectRandomNoise(corrupted);
        }
        
        return corrupted;
    }
    
    /**
     * ENHANCED: Simulates burst noise with SYNC WORD TARGETING
     * Solar interference often hits the beginning of transmission
     */
    public static byte[] injectBurstNoise(byte[] data) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        
        // STRATEGIC: 70% chance to target sync word area (positions 0-10)
        boolean targetSyncArea = random.nextDouble() < 0.7;
        int burstStart, burstLength;
        
        if (targetSyncArea && data.length > 10) {
            // Target sync word or immediate header (positions 0-10)
            burstStart = random.nextInt(8); // 0-7 to hit sync word
            burstLength = 2 + random.nextInt(4); // 2-5 bytes
            System.out.printf("🌋 SYNC-TARGETING BURST NOISE: %d bytes at SYNC position %d\n", 
                             burstLength, burstStart);
        } else {
            // Random burst elsewhere
            burstStart = random.nextInt(data.length - 6);
            burstLength = 2 + random.nextInt(5);
            System.out.printf("🌋 RANDOM BURST NOISE: %d bytes at position %d\n", 
                             burstLength, burstStart);
        }
        
        // Flip all bits in the burst region - MORE SEVERE
        for (int i = burstStart; i < burstStart + burstLength && i < data.length; i++) {
            corrupted[i] = (byte) ~corrupted[i]; // Flip all bits
            // Additional random corruption for more severity
            if (random.nextDouble() < 0.3) {
                corrupted[i] ^= (byte) (1 << random.nextInt(8));
            }
        }
        
        return corrupted;
    }
    
    /**
     * ENHANCED: Simulates sync word drift/misalignment with VARIABLE SHIFTS
     */
    public static byte[] injectHeaderDrift(byte[] data) {
        // Variable shift: 1-4 bytes, but sometimes larger for severe cases
        int shift = 1 + random.nextInt(4);
        if (random.nextDouble() < 0.2) { // 20% chance for severe drift
            shift = 5 + random.nextInt(6); // 5-10 byte shift
        }
        
        byte[] shifted = new byte[data.length + shift];
        
        // Add realistic noise preamble (not completely random)
        for (int i = 0; i < shift; i++) {
            // More realistic noise pattern (alternating, not pure random)
            if (i % 2 == 0) {
                shifted[i] = (byte) 0xAA; // Patterned noise
            } else {
                shifted[i] = (byte) 0x55; // Alternating pattern
            }
        }
        
        // Copy original data after the shift
        System.arraycopy(data, 0, shifted, shift, data.length);
        
        System.out.printf("🌀 HEADER DRIFT: %d byte shift with patterned noise\n", shift);
        return shifted;
    }
    
    /**
     * ENHANCED: Simulates packet truncation with STRATEGIC CUT POINTS
     */
    public static byte[] injectPacketTruncation(byte[] data) {
        // Strategic cut points: header, middle of payload, or severe truncation
        double strategy = random.nextDouble();
        int truncatePoint;
        
        if (strategy < 0.4) {
            // Cut in header area (severe)
            truncatePoint = TelemetryFrame.SYNC_SIZE + random.nextInt(8);
            System.out.printf("✂️  HEADER TRUNCATION: cut at byte %d (severe)\n", truncatePoint);
        } else if (strategy < 0.7) {
            // Cut in middle of payload
            int payloadStart = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE;
            truncatePoint = payloadStart + random.nextInt(data.length - payloadStart - 10);
            System.out.printf("✂️  PAYLOAD TRUNCATION: cut at byte %d\n", truncatePoint);
        } else {
            // Severe truncation (only sync word remains)
            truncatePoint = TelemetryFrame.SYNC_SIZE + 2;
            System.out.printf("✂️  SEVERE TRUNCATION: only %d bytes remain\n", truncatePoint);
        }
        
        // Ensure we don't truncate to zero
        truncatePoint = Math.max(4, Math.min(truncatePoint, data.length - 1));
        byte[] truncated = Arrays.copyOf(data, truncatePoint);
        
        return truncated;
    }
    
    /**
     * NEW: Targeted bit flips for STRUCTURAL IMPACT
     * Specifically attacks critical frame fields
     */
    public static byte[] injectTargetedBitFlips(byte[] data) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        int flipCount = 0;
        
        // STRATEGIC: Target critical structural fields
        int[] criticalPositions = {
            TelemetryFrame.POS_SYNC,           // Sync word
            TelemetryFrame.POS_SATELLITE_ID,   // Satellite ID  
            TelemetryFrame.POS_TIMESTAMP,      // Timestamp
            TelemetryFrame.POS_PAYLOAD_LENGTH, // Payload length (CRITICAL!)
            TelemetryFrame.POS_PAYLOAD_LENGTH + 1
        };
        
        // Flip bits in critical positions with high probability
        for (int pos : criticalPositions) {
            if (pos < corrupted.length && random.nextDouble() < 0.6) {
                int bitToFlip = random.nextInt(8);
                corrupted[pos] ^= (1 << bitToFlip);
                flipCount++;
                System.out.printf("⚡ CRITICAL BIT FLIP: position %d, bit %d\n", pos, bitToFlip);
            }
        }
        
        // Additional random flips throughout frame
        for (int i = 0; i < corrupted.length && flipCount < 5; i++) {
            if (random.nextDouble() < 0.02) { // 2% chance per byte
                int bitToFlip = random.nextInt(8);
                corrupted[i] ^= (1 << bitToFlip);
                flipCount++;
            }
        }
        
        System.out.printf("⚡ TARGETED BIT FLIPS: %d critical bits flipped\n", flipCount);
        return corrupted;
    }
    
    /**
     * ENHANCED: Simulates random noise injection with REALISTIC PATTERNS
     */
    public static byte[] injectRandomNoise(byte[] data) {
        // Variable noise length: 2-10 bytes
        int noiseLength = 2 + random.nextInt(9);
        byte[] noisy = new byte[data.length + noiseLength];
        
        // Realistic noise patterns (not pure random)
        for (int i = 0; i < noiseLength; i++) {
            switch (i % 4) {
                case 0: noisy[i] = (byte) 0xFF; break; // All ones
                case 1: noisy[i] = (byte) 0x00; break; // All zeros  
                case 2: noisy[i] = (byte) 0xAA; break; // Alternating 1/0
                case 3: noisy[i] = (byte) 0x55; break; // Alternating 0/1
            }
        }
        
        // Copy original data after noise
        System.arraycopy(data, 0, noisy, noiseLength, data.length);
        
        System.out.printf("📡 RANDOM NOISE: %d bytes of patterned preamble\n", noiseLength);
        return noisy;
    }
    
    /**
     * NEW: Inject specific error type for testing
     */
    public static byte[] injectSpecificError(byte[] data, String errorType) {
        switch (errorType.toUpperCase()) {
            case "BURST_SYNC":
                return injectBurstNoiseTargeted(data, 0, 4); // Target sync word
            case "BURST_HEADER":
                return injectBurstNoiseTargeted(data, 4, 8); // Target header
            case "SEVERE_TRUNCATION":
                return Arrays.copyOf(data, 8); // Only 8 bytes
            case "PAYLOAD_CORRUPTION":
                return injectPayloadCorruption(data);
            case "COMPLETE_CORRUPTION":
                return injectCompleteCorruption(data);
            default:
                return applyTransmissionErrors(data);
        }
    }
    
    /**
     * Target burst noise to specific area
     */
    private static byte[] injectBurstNoiseTargeted(byte[] data, int start, int length) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        int burstLength = 2 + random.nextInt(length - 2);
        
        for (int i = start; i < start + burstLength && i < data.length; i++) {
            corrupted[i] = (byte) ~corrupted[i]; // Flip all bits
        }
        
        System.out.printf("🎯 TARGETED BURST: %d bytes at %d-%d\n", 
                         burstLength, start, start + burstLength);
        return corrupted;
    }
    
    /**
     * Corrupt payload specifically (affects data but not structure)
     */
    private static byte[] injectPayloadCorruption(byte[] data) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        int payloadStart = TelemetryFrame.POS_PAYLOAD;
        
        if (payloadStart < corrupted.length) {
            int payloadEnd = Math.min(corrupted.length, payloadStart + 20);
            for (int i = payloadStart; i < payloadEnd; i++) {
                if (random.nextDouble() < 0.3) {
                    corrupted[i] = (byte) random.nextInt(256);
                }
            }
            System.out.println("📦 PAYLOAD CORRUPTION: Data content altered");
        }
        
        return corrupted;
    }
    
    /**
     * Complete frame corruption (worst case)
     */
    private static byte[] injectCompleteCorruption(byte[] data) {
        byte[] corrupted = new byte[data.length];
        for (int i = 0; i < corrupted.length; i++) {
            corrupted[i] = (byte) random.nextInt(256);
        }
        System.out.println("💀 COMPLETE CORRUPTION: Entire frame destroyed");
        return corrupted;
    }
    
    /**
     * Demo method to test all enhanced error types
     */
    public static void demo() {
        System.out.println("🔬 ENHANCED CORRUPTION SIMULATOR DEMO");
        System.out.println("=====================================");
        
        // Create a sample frame
        TelemetryFrame frame = FrameBuilder.createSampleFrame();
        byte[] original = frame.toBinary();
        
        System.out.printf("Original frame: %d bytes\n\n", original.length);
        
        // Test each enhanced error type
        testEnhancedErrorType("SYNC-TARGETING BURST", original, 
            data -> injectBurstNoise(data));
            
        testEnhancedErrorType("HEADER DRIFT", original,
            data -> injectHeaderDrift(data));
            
        testEnhancedErrorType("TARGETED BIT FLIPS", original,
            data -> injectTargetedBitFlips(data));
            
        testEnhancedErrorType("STRATEGIC TRUNCATION", original,
            data -> injectPacketTruncation(data));
    }
    
    private static void testEnhancedErrorType(String name, byte[] original, 
                                            java.util.function.Function<byte[], byte[]> errorFunc) {
        System.out.printf("\n--- Testing %s ---\n", name);
        byte[] corrupted = errorFunc.apply(original);
        
        // Validate the result
        FrameValidator.ValidationReport report = FrameValidator.validateCompleteFrame(corrupted);
        
        System.out.printf("Size: %d → %d bytes | Result: %s | Confidence: %.1f%%\n",
                         original.length, corrupted.length, report.overallStatus, 
                         report.confidenceScore * 100);
        
        if (report.wasResynced) {
            System.out.println("🔄 Frame was resynchronized");
        }
    }
}