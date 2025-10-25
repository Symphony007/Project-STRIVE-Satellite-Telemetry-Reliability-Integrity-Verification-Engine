package com.aerospace.strive.transmission;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents a realistic satellite telemetry frame with binary encoding
 * Simulates actual satellite-to-ground transmission format
 */
public class TelemetryFrame {
    
    // Constants for frame structure
    public static final int SYNC_WORD = 0x1ACFFC1D;  // Fixed sync pattern
    public static final int SYNC_SIZE = 4;           // bytes
    public static final int HEADER_SIZE = 12;        // bytes  
    public static final int CRC_SIZE = 4;            // bytes
    public static final int MAX_PAYLOAD_SIZE = 64;   // bytes
    
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
        
        // 4. CRC32 (4 bytes)
        buffer.putInt((int) checksum);
        
        return buffer.array();
    }
    
    /**
     * Calculates CRC32 checksum over header + payload
     */
    private long calculateChecksum() {
        // For now, we'll use a simple simulation
        // In Microstep 2, we'll integrate your real ChecksumUtil
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        
        ByteBuffer tempBuffer = ByteBuffer.allocate(HEADER_SIZE + payload.length);
        tempBuffer.putShort((short) satelliteId);
        tempBuffer.putLong(timestamp);
        tempBuffer.putShort((short) payload.length);
        tempBuffer.put(payload);
        
        crc.update(tempBuffer.array());
        return crc.getValue();
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
}