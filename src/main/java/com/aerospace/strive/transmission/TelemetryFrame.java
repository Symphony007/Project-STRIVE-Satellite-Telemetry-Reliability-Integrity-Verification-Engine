package com.aerospace.strive.transmission;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Represents a realistic satellite telemetry frame with binary encoding
 * AND FRAME VALIDATION CAPABILITIES
 * Simulates actual satellite-to-ground transmission format
 * FIXED: CRC calculation bug - now matches CRCErrorDetector
 */
public class TelemetryFrame {
    
    // Constants for frame structure
    public static final int SYNC_WORD = 0x1ACFFC1D;  // Fixed sync pattern
    public static final int SYNC_SIZE = 4;           // bytes
    public static final int HEADER_SIZE = 12;        // bytes  
    public static final int CRC_SIZE = 4;            // bytes
    public static final int MAX_PAYLOAD_SIZE = 64;   // bytes
    
    // Frame structure positions
    public static final int POS_SYNC = 0;
    public static final int POS_SATELLITE_ID = 4;
    public static final int POS_TIMESTAMP = 6;
    public static final int POS_PAYLOAD_LENGTH = 14;
    public static final int POS_PAYLOAD = 16;
    
    private final int satelliteId;
    private final long timestamp;
    private final int frameCounter;
    private final byte[] payload;  // Telemetry data in binary
    private final long checksum;
    
    public TelemetryFrame(int satelliteId, long timestamp, int frameCounter, byte[] payload) {
        this.satelliteId = satelliteId;
        this.timestamp = timestamp;
        this.frameCounter = frameCounter;
        this.payload = Arrays.copyOf(payload, payload.length);
        this.checksum = calculateChecksum();
    }
    
    /**
     * Converts the entire frame to binary format for transmission
     * FIXED: Proper CRC calculation and placement
     */
    public byte[] toBinary() {
        int payloadLength = payload.length;
        int totalSize = SYNC_SIZE + HEADER_SIZE + payloadLength + CRC_SIZE;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        // 1. SYNC WORD (4 bytes)
        buffer.putInt(SYNC_WORD);
        
        // 2. HEADER (12 bytes)
        buffer.putShort((short) satelliteId);     // 2 bytes - Satellite ID
        buffer.putLong(timestamp);                // 8 bytes - Unix timestamp
        buffer.putShort((short) payloadLength);   // 2 bytes - Payload length
        
        // 3. PAYLOAD (variable)
        buffer.put(payload);
        
        // 4. CRC32 (4 bytes) - Calculate over header + payload ONLY (not sync word)
        byte[] dataForCRC = getDataForCRCCalculation();
        long calculatedCRC = calculateCRC32(dataForCRC);
        buffer.putInt((int) calculatedCRC);
        
        return buffer.array();
    }
    
    /**
     * FIXED: Get the correct data portion for CRC calculation
     * CRC should cover HEADER + PAYLOAD only (not sync word)
     */
    private byte[] getDataForCRCCalculation() {
        int dataSize = HEADER_SIZE + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(dataSize);
        
        // Header
        buffer.putShort((short) satelliteId);
        buffer.putLong(timestamp);
        buffer.putShort((short) payload.length);
        
        // Payload
        buffer.put(payload);
        
        return buffer.array();
    }
    
    /**
     * FIXED: Standard CRC-32 calculation that matches CRCErrorDetector
     */
    private long calculateChecksum() {
        byte[] dataForCRC = getDataForCRCCalculation();
        return calculateCRC32(dataForCRC);
    }
    
    /**
     * Standard CRC-32 calculation that matches across all components
     */
    public static long calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        if (data != null && data.length > 0) {
            crc.update(data);
        }
        return crc.getValue();
    }
    
    /**
     * LAYER 1 VALIDATION: Frame Synchronization Check
     * Detects header drift and sync loss using correlation
     * Returns: SYNC_VALID, SYNC_WEAK, or SYNC_LOST
     */
    public static String validateFrameSync(byte[] receivedData) {
        if (receivedData == null || receivedData.length < SYNC_SIZE) {
            return "SYNC_LOST"; // Not enough data for sync word
        }
        
        // Extract received sync word
        ByteBuffer buffer = ByteBuffer.wrap(receivedData, 0, SYNC_SIZE);
        int receivedSync = buffer.getInt();
        
        // Perfect match
        if (receivedSync == SYNC_WORD) {
            return "SYNC_VALID";
        }
        
        // Calculate bitwise correlation (Hamming distance)
        int correlation = calculateSyncCorrelation(receivedSync, SYNC_WORD);
        int maxCorrelation = 32; // 32 bits in sync word
        
        // Strong correlation (up to 2 bit errors)
        if (correlation >= 30) {
            return "SYNC_WEAK"; // Sync found but with minor errors
        }
        // Moderate correlation (3-6 bit errors)  
        else if (correlation >= 26) {
            return "SYNC_DEGRADED";
        }
        // Weak or no correlation
        else {
            return "SYNC_LOST";
        }
    }
    
    /**
     * Calculate correlation between received and expected sync words
     * Returns number of matching bits (0-32)
     */
    private static int calculateSyncCorrelation(int received, int expected) {
        int xor = received ^ expected;
        // Count number of 0 bits (matching bits) in XOR result
        return 32 - Integer.bitCount(xor);
    }
    
    /**
     * LAYER 1 VALIDATION: Frame Length & Structure Check
     * Detects packet truncation and malformed frames
     * Returns: LENGTH_VALID, TRUNCATED, or MALFORMED
     */
    public static String validateFrameStructure(byte[] receivedData) {
        if (receivedData == null) {
            return "MALFORMED";
        }
        
        // Minimum frame size check
        int minFrameSize = SYNC_SIZE + HEADER_SIZE + CRC_SIZE;
        if (receivedData.length < minFrameSize) {
            return "TRUNCATED"; // Not enough for basic frame structure
        }
        
        try {
            // Extract payload length from header
            if (receivedData.length < POS_PAYLOAD_LENGTH + 2) {
                return "TRUNCATED";
            }
            
            ByteBuffer buffer = ByteBuffer.wrap(receivedData, POS_PAYLOAD_LENGTH, 2);
            int declaredPayloadLength = buffer.getShort() & 0xFFFF;
            
            // Calculate expected total frame size
            int expectedSize = SYNC_SIZE + HEADER_SIZE + declaredPayloadLength + CRC_SIZE;
            
            if (receivedData.length == expectedSize) {
                return "LENGTH_VALID";
            } else if (receivedData.length < expectedSize) {
                return "TRUNCATED";
            } else {
                return "OVERSIZED"; // More data than expected
            }
            
        } catch (Exception e) {
            return "MALFORMED"; // Invalid header data
        }
    }
    
    /**
     * LAYER 1 VALIDATION: Complete Frame Integrity Check
     * Combines sync and structure validation
     * Returns detailed validation result
     */
    public static FrameValidationResult validateFrameIntegrity(byte[] receivedData) {
        String syncStatus = validateFrameSync(receivedData);
        String structureStatus = validateFrameStructure(receivedData);
        
        // Determine overall status
        String overallStatus;
        if ("SYNC_VALID".equals(syncStatus) && "LENGTH_VALID".equals(structureStatus)) {
            overallStatus = "VALID";
        } else if ("SYNC_LOST".equals(syncStatus)) {
            overallStatus = "SYNC_LOST";
        } else if ("TRUNCATED".equals(structureStatus)) {
            overallStatus = "TRUNCATED";
        } else if ("MALFORMED".equals(structureStatus)) {
            overallStatus = "MALFORMED";
        } else {
            overallStatus = "DEGRADED";
        }
        
        return new FrameValidationResult(overallStatus, syncStatus, structureStatus);
    }
    
    /**
     * Attempt to resynchronize a frame with weak sync
     * Returns corrected frame data or null if cannot resync
     */
    public static byte[] attemptFrameResync(byte[] corruptedData) {
        if (corruptedData == null || corruptedData.length < SYNC_SIZE + HEADER_SIZE + CRC_SIZE) {
            return null; // Not enough data to work with
        }
        
        // Strategy 1: Check if sync word appears elsewhere in the data
        byte[] syncBytes = ByteBuffer.allocate(4).putInt(SYNC_WORD).array();
        
        for (int i = 1; i <= 10 && i < corruptedData.length - 4; i++) {
            boolean syncFound = true;
            for (int j = 0; j < 4; j++) {
                if (corruptedData[i + j] != syncBytes[j]) {
                    syncFound = false;
                    break;
                }
            }
            
            if (syncFound) {
                // Found sync word at position i, realign frame
                int newLength = corruptedData.length - i;
                if (newLength >= SYNC_SIZE + HEADER_SIZE + CRC_SIZE) {
                    byte[] realigned = new byte[newLength];
                    System.arraycopy(corruptedData, i, realigned, 0, newLength);
                    System.out.println("🔄 Frame resynchronized: sync word found at offset " + i);
                    return realigned;
                }
            }
        }
        
        return null; // Cannot resync
    }
    
    // Getters
    public int getSatelliteId() { return satelliteId; }
    public long getTimestamp() { return timestamp; }
    public int getFrameCounter() { return frameCounter; }
    public byte[] getPayload() { return Arrays.copyOf(payload, payload.length); }
    public long getChecksum() { return checksum; }
    
    @Override
    public String toString() {
        return String.format("TelemetryFrame[ID=%d, Time=%d, Counter=%d, Payload=%d bytes, CRC=%08X]",
                           satelliteId, timestamp, frameCounter, payload.length, checksum);
    }
    
    /**
     * Validation result container for detailed error reporting
     */
    public static class FrameValidationResult {
        public final String overallStatus;
        public final String syncStatus;
        public final String structureStatus;
        public final long timestamp;
        
        public FrameValidationResult(String overallStatus, String syncStatus, String structureStatus) {
            this.overallStatus = overallStatus;
            this.syncStatus = syncStatus;
            this.structureStatus = structureStatus;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return "VALID".equals(overallStatus);
        }
        
        public boolean needsResync() {
            return "SYNC_LOST".equals(overallStatus) || "SYNC_WEAK".equals(syncStatus);
        }
        
        @Override
        public String toString() {
            return String.format("Validation[%s] Sync:%s Structure:%s", 
                               overallStatus, syncStatus, structureStatus);
        }
    }
}