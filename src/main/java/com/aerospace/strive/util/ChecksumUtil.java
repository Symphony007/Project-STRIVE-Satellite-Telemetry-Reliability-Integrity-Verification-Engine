package com.aerospace.strive.util;

import java.util.zip.CRC32;

public class ChecksumUtil {
    
    /**
     * Calculate CRC32 checksum for data integrity verification
     */
    public static long calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
    
    /**
     * Validate if data matches the expected checksum
     */
    public static boolean validateChecksum(byte[] data, long expectedChecksum) {
        long actualChecksum = calculateCRC32(data);
        return actualChecksum == expectedChecksum;
    }
    
    /**
     * Simulate data corruption by flipping random bits
     * Used for testing error detection
     */
    public static byte[] injectErrors(byte[] originalData, double errorRate) {
        byte[] corrupted = originalData.clone();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < corrupted.length; i++) {
            if (random.nextDouble() < errorRate) {
                // Flip a random bit in this byte
                int bitToFlip = random.nextInt(8);
                corrupted[i] ^= (1 << bitToFlip);
            }
        }
        return corrupted;
    }
}