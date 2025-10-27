package com.aerospace.strive.transmission;

import java.util.zip.CRC32;

/**
 * LAYER 2: CRC ERROR DETECTION - ENHANCED VERSION
 * Detects bit errors, data corruption, and payload integrity issues
 * FIXED: Proper handling of header drift and random noise scenarios
 */
public class CRCErrorDetector {
    
    // NASA-standard CRC-32 polynomial
    private static final long CRC32_POLYNOMIAL = 0x04C11DB7L;
    
    /**
     * ENHANCED: Comprehensive Layer 2 validation with header drift support
     * Now handles frames that have been resynchronized or have noise prefixes
     */
    public static CRCValidationReport validateDataIntegrity(byte[] frameData) {
        if (frameData == null || frameData.length < TelemetryFrame.CRC_SIZE) {
            return new CRCValidationReport("INVALID", "Insufficient data for CRC check", 0, 0);
        }
        
        // FIXED: First attempt to find the actual frame start using sync word
        Integer frameStart = findFrameStartPosition(frameData);
        if (frameStart == null) {
            return new CRCValidationReport("SYNC_LOST", "Cannot locate frame start for CRC check", 0, 0);
        }
        
        // Extract data and checksum from the correctly aligned frame
        byte[] dataForCRC = extractDataForCRCCalculation(frameData, frameStart);
        long receivedChecksum = extractChecksum(frameData, frameStart);
        
        // Calculate expected checksum
        long calculatedChecksum = TelemetryFrame.calculateCRC32(dataForCRC);
        
        // Verify checksum
        boolean checksumValid = (receivedChecksum == calculatedChecksum);
        
        // Analyze error patterns if checksum fails
        String diagnosis = checksumValid ? 
            "Data integrity verified" : 
            analyzeCRCErrorPattern(dataForCRC, receivedChecksum, calculatedChecksum);
        
        // Calculate bit error rate estimate
        double errorConfidence = calculateErrorConfidence(dataForCRC, 
                                                        receivedChecksum, calculatedChecksum);
        
        // Add frame alignment info to diagnosis if needed
        if (frameStart > 0) {
            diagnosis += " (frame realigned from offset " + frameStart + ")";
        }
        
        return new CRCValidationReport(
            checksumValid ? "CRC_VALID" : "CRC_FAIL",
            diagnosis,
            receivedChecksum,
            calculatedChecksum,
            errorConfidence
        );
    }
    
    /**
     * FIXED: Find the actual start of the frame by locating sync word
     * This handles header drift and random noise scenarios
     */
    private static Integer findFrameStartPosition(byte[] frameData) {
        if (frameData == null || frameData.length < TelemetryFrame.SYNC_SIZE) {
            return null;
        }
        
        byte[] syncBytes = java.nio.ByteBuffer.allocate(4).putInt(TelemetryFrame.SYNC_WORD).array();
        
        // Search for sync word in the data
        for (int i = 0; i <= frameData.length - TelemetryFrame.SYNC_SIZE; i++) {
            boolean syncFound = true;
            for (int j = 0; j < TelemetryFrame.SYNC_SIZE; j++) {
                if (frameData[i + j] != syncBytes[j]) {
                    syncFound = false;
                    break;
                }
            }
            
            if (syncFound) {
                // Found sync word, verify we have enough data for full frame
                int remainingData = frameData.length - i;
                int minFrameSize = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + TelemetryFrame.CRC_SIZE;
                
                if (remainingData >= minFrameSize) {
                    return i; // Return the frame start position
                }
            }
        }
        
        return null; // Sync word not found
    }
    
    /**
     * FIXED: Extract data for CRC calculation from properly aligned frame
     */
    private static byte[] extractDataForCRCCalculation(byte[] frameData, int frameStart) {
        // Skip sync word (first 4 bytes after frame start), take header + payload
        int dataStart = frameStart + TelemetryFrame.SYNC_SIZE;
        int dataLength = (frameData.length - frameStart) - TelemetryFrame.SYNC_SIZE - TelemetryFrame.CRC_SIZE;
        
        if (dataLength <= 0) {
            return new byte[0];
        }
        
        byte[] dataForCRC = new byte[dataLength];
        System.arraycopy(frameData, dataStart, dataForCRC, 0, dataLength);
        return dataForCRC;
    }
    
    /**
     * FIXED: Extract checksum from properly aligned frame
     */
    private static long extractChecksum(byte[] frameData, int frameStart) {
        int crcPosition = frameStart + (frameData.length - frameStart - TelemetryFrame.CRC_SIZE);
        
        if (crcPosition + TelemetryFrame.CRC_SIZE > frameData.length) {
            return 0; // Invalid position
        }
        
        return ((frameData[crcPosition] & 0xFFL) << 24) |
               ((frameData[crcPosition + 1] & 0xFFL) << 16) |
               ((frameData[crcPosition + 2] & 0xFFL) << 8) |
               (frameData[crcPosition + 3] & 0xFFL);
    }
    
    /**
     * Calculate CRC-32 checksum using NASA-standard algorithm
     */
    public static long calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        if (data != null && data.length > 0) {
            crc.update(data);
        }
        return crc.getValue();
    }
    
    /**
     * Analyze CRC error patterns to diagnose likely error type
     */
    private static String analyzeCRCErrorPattern(byte[] data, long receivedCRC, long calculatedCRC) {
        if (data == null || data.length == 0) {
            return "No data to analyze";
        }
        
        long xor = receivedCRC ^ calculatedCRC;
        int errorBits = Long.bitCount(xor);
        
        // Analyze error pattern
        if (errorBits == 1) {
            return "Single-bit error detected";
        } else if (errorBits <= 3) {
            return "Few bit errors (" + errorBits + " bits) - likely random noise";
        } else if (errorBits <= 8) {
            return "Multiple bit errors (" + errorBits + " bits) - possible burst start";
        } else {
            return "Severe data corruption (" + errorBits + " error bits)";
        }
    }
    
    /**
     * Calculate confidence score for error detection (0.0 to 1.0)
     */
    private static double calculateErrorConfidence(byte[] data, long receivedCRC, long calculatedCRC) {
        if (receivedCRC == calculatedCRC) {
            return 1.0; // Perfect match
        }
        
        long xor = receivedCRC ^ calculatedCRC;
        int errorBits = Long.bitCount(xor);
        
        // More error bits = higher confidence in detection
        double confidence = Math.min(0.95, errorBits / 32.0);
        
        // Adjust based on data length (more data = more reliable CRC)
        if (data != null) {
            double dataFactor = Math.min(1.0, data.length / 100.0);
            confidence *= (0.3 + 0.7 * dataFactor);
        }
        
        return confidence;
    }
    
    /**
     * Simulate CRC protection by adding checksum to data
     */
    public static byte[] addCRCProtection(byte[] data) {
        if (data == null) return null;
        
        long checksum = calculateCRC32(data);
        byte[] protectedData = new byte[data.length + TelemetryFrame.CRC_SIZE];
        
        // Copy original data
        System.arraycopy(data, 0, protectedData, 0, data.length);
        
        // Append CRC
        protectedData[data.length] = (byte) ((checksum >> 24) & 0xFF);
        protectedData[data.length + 1] = (byte) ((checksum >> 16) & 0xFF);
        protectedData[data.length + 2] = (byte) ((checksum >> 8) & 0xFF);
        protectedData[data.length + 3] = (byte) (checksum & 0xFF);
        
        return protectedData;
    }
    
    /**
     * Verify if data matches expected checksum
     */
    public static boolean verifyCRC(byte[] data, long expectedChecksum) {
        return calculateCRC32(data) == expectedChecksum;
    }
    
    /**
     * Estimate bit error rate based on CRC mismatch
     */
    public static double estimateBitErrorRate(byte[] data, long receivedCRC, long calculatedCRC) {
        if (data == null || data.length == 0) return 0.0;
        
        long xor = receivedCRC ^ calculatedCRC;
        int errorBits = Long.bitCount(xor);
        
        // This is a rough estimate - actual BER would require more analysis
        return (double) errorBits / (data.length * 8.0);
    }
    
    /**
     * Quick validation for real-time processing
     */
    public static boolean quickValidate(byte[] frameData) {
        if (frameData == null) return false;
        
        CRCValidationReport report = validateDataIntegrity(frameData);
        return report.isValid();
    }
    
    /**
     * CRC Validation Result Container
     */
    public static class CRCValidationReport {
        public final String status;
        public final String diagnosis;
        public final long receivedChecksum;
        public final long calculatedChecksum;
        public final double confidence;
        public final long timestamp;
        
        public CRCValidationReport(String status, String diagnosis, 
                                 long receivedChecksum, long calculatedChecksum, double confidence) {
            this.status = status;
            this.diagnosis = diagnosis;
            this.receivedChecksum = receivedChecksum;
            this.calculatedChecksum = calculatedChecksum;
            this.confidence = confidence;
            this.timestamp = System.currentTimeMillis();
        }
        
        public CRCValidationReport(String status, String diagnosis, 
                                 long receivedChecksum, long calculatedChecksum) {
            this(status, diagnosis, receivedChecksum, calculatedChecksum, 1.0);
        }
        
        public boolean isValid() {
            return "CRC_VALID".equals(status);
        }
        
        public boolean hasErrors() {
            return !isValid();
        }
        
        public double getBitErrorRateEstimate() {
            return estimateBitErrorRate(null, receivedChecksum, calculatedChecksum);
        }
        
        @Override
        public String toString() {
            return String.format("CRCValidation[%s] Confidence:%.1f%% Diagnosis:%s", 
                               status, confidence * 100, diagnosis);
        }
    }
}