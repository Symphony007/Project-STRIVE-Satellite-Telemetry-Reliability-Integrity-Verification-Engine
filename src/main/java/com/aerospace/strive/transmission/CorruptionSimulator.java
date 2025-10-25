package com.aerospace.strive.transmission;

import java.util.Arrays;
import java.util.Random;

/**
 * Simulates realistic satellite transmission errors
 * Based on actual RF impairments in space communications
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
     * Returns the corrupted frame bytes
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
            corrupted = injectBitFlips(corrupted, 0.02); // 2% bit error rate
        }
        
        if (random.nextDouble() < RANDOM_NOISE_PROB) {
            corrupted = injectRandomNoise(corrupted);
        }
        
        return corrupted;
    }
    
    /**
     * Simulates burst noise (solar interference) - flips consecutive bytes
     */
    private static byte[] injectBurstNoise(byte[] data) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        
        // Choose burst location and length (2-6 consecutive bytes)
        int burstStart = random.nextInt(data.length - 6);
        int burstLength = 2 + random.nextInt(5);
        
        // Flip all bits in the burst region
        for (int i = burstStart; i < burstStart + burstLength && i < data.length; i++) {
            corrupted[i] = (byte) ~corrupted[i]; // Flip all bits
        }
        
        System.out.printf("🌪️  Injected BURST NOISE: %d bytes at position %d\n", 
                         burstLength, burstStart);
        return corrupted;
    }
    
    /**
     * Simulates sync word drift/misalignment
     */
    private static byte[] injectHeaderDrift(byte[] data) {
        // Shift entire frame right by 1-3 bytes, padding with garbage
        int shift = 1 + random.nextInt(3);
        byte[] shifted = new byte[data.length + shift];
        
        // Add random preamble (simulates noise before sync)
        for (int i = 0; i < shift; i++) {
            shifted[i] = (byte) random.nextInt(256);
        }
        
        // Copy original data after the shift
        System.arraycopy(data, 0, shifted, shift, data.length);
        
        System.out.printf("🌀 Injected HEADER DRIFT: %d byte shift\n", shift);
        return shifted;
    }
    
    /**
     * Simulates packet truncation (transmission cut off)
     */
    private static byte[] injectPacketTruncation(byte[] data) {
        // Cut off random portion from the end (20-80% of packet)
        int truncatePoint = data.length - (data.length / 5) - random.nextInt(data.length / 2);
        byte[] truncated = Arrays.copyOf(data, truncatePoint);
        
        System.out.printf("✂️  Injected PACKET TRUNCATION: cut from %d to %d bytes\n", 
                         data.length, truncatePoint);
        return truncated;
    }
    
    /**
     * Simulates random bit flips (cosmic radiation)
     */
    private static byte[] injectBitFlips(byte[] data, double errorRate) {
        byte[] corrupted = Arrays.copyOf(data, data.length);
        int flipCount = 0;
        
        for (int i = 0; i < corrupted.length; i++) {
            if (random.nextDouble() < errorRate) {
                int bitToFlip = random.nextInt(8);
                corrupted[i] ^= (1 << bitToFlip);
                flipCount++;
            }
        }
        
        System.out.printf("⚡ Injected BIT FLIPS: %d bits flipped\n", flipCount);
        return corrupted;
    }
    
    /**
     * Simulates random noise injection before valid data
     */
    private static byte[] injectRandomNoise(byte[] data) {
        // Add 2-8 bytes of garbage before the actual frame
        int noiseLength = 2 + random.nextInt(7);
        byte[] noisy = new byte[data.length + noiseLength];
        
        // Fill with random bytes
        for (int i = 0; i < noiseLength; i++) {
            noisy[i] = (byte) random.nextInt(256);
        }
        
        // Copy original data after noise
        System.arraycopy(data, 0, noisy, noiseLength, data.length);
        
        System.out.printf("📡 Injected RANDOM NOISE: %d byte preamble\n", noiseLength);
        return noisy;
    }
    
    /**
     * Demo method to test all error types
     */
    public static void demo() {
        System.out.println("🧪 Corruption Simulator Demo");
        System.out.println("=============================");
        
        // Create a sample frame
        TelemetryFrame frame = FrameBuilder.createSampleFrame();
        byte[] original = frame.toBinary();
        
        System.out.printf("Original frame: %d bytes\n", original.length);
        
        // Test each error type
        testErrorType("BURST NOISE", original, 
            data -> injectBurstNoise(data));
            
        testErrorType("HEADER DRIFT", original,
            data -> injectHeaderDrift(data));
            
        testErrorType("PACKET TRUNCATION", original,
            data -> injectPacketTruncation(data));
    }
    
    private static void testErrorType(String name, byte[] original, 
                                    java.util.function.Function<byte[], byte[]> errorFunc) {
        System.out.printf("\n--- Testing %s ---\n", name);
        byte[] corrupted = errorFunc.apply(original);
        System.out.printf("Result: %d → %d bytes\n", original.length, corrupted.length);
    }
}