package com.aerospace.strive.transmission;

/**
 * LAYER 1: Comprehensive Frame Validator - ENHANCED VERSION
 * Handles structural integrity checks, sync validation, and error diagnosis
 * FIXED: Now detects and reports recovered frames properly
 */
public class FrameValidator {
    
    // Validation thresholds (configurable)
    private static final double STRONG_SYNC_THRESHOLD = 0.94;    // 30/32 bits
    private static final double WEAK_SYNC_THRESHOLD = 0.81;      // 26/32 bits
    private static final int MAX_RESYNC_ATTEMPTS = 3;
    
    /**
     * ENHANCED: Complete Layer 1 validation pipeline with recovery detection
     * Returns detailed diagnosis of frame integrity including recovery status
     */
    public static ValidationReport validateCompleteFrame(byte[] frameData) {
        if (frameData == null) {
            return new ValidationReport("INVALID", "NULL_DATA", "No data provided", 0, false, null, false);
        }
        
        // Step 1: Initial frame integrity check
        TelemetryFrame.FrameValidationResult integrityCheck = 
            TelemetryFrame.validateFrameIntegrity(frameData);
        
        // Step 2: If sync is weak or lost, attempt resynchronization
        byte[] correctedData = frameData;
        int resyncAttempts = 0;
        boolean wasResynced = false;
        boolean hadStructuralIssues = !integrityCheck.isValid();
        
        if (integrityCheck.needsResync() && resyncAttempts < MAX_RESYNC_ATTEMPTS) {
            correctedData = TelemetryFrame.attemptFrameResync(frameData);
            if (correctedData != null) {
                // Re-validate after resync
                integrityCheck = TelemetryFrame.validateFrameIntegrity(correctedData);
                wasResynced = true;
                resyncAttempts++;
            }
        }
        
        // Step 3: Analyze error patterns for diagnosis
        String errorDiagnosis = diagnoseErrorPattern(frameData, integrityCheck, wasResynced);
        
        // Step 4: Calculate confidence score (0.0 to 1.0)
        double confidenceScore = calculateConfidenceScore(integrityCheck, wasResynced, hadStructuralIssues);
        
        // Step 5: Determine final status - FIXED: Handle recovered frames properly
        String finalStatus = determineFinalStatus(integrityCheck, wasResynced, hadStructuralIssues);
        
        return new ValidationReport(
            finalStatus,
            integrityCheck.syncStatus,
            errorDiagnosis,
            confidenceScore,
            wasResynced,
            correctedData,
            hadStructuralIssues
        );
    }
    
    /**
     * FIXED: Determine final status accounting for recovery
     */
    private static String determineFinalStatus(TelemetryFrame.FrameValidationResult integrityCheck, 
                                             boolean wasResynced, boolean hadStructuralIssues) {
        
        if (integrityCheck.isValid()) {
            if (wasResynced && hadStructuralIssues) {
                return "RECOVERED"; // Was corrupted but successfully recovered
            } else {
                return "VALID"; // Genuinely valid from start
            }
        } else {
            return integrityCheck.overallStatus; // Still has issues
        }
    }
    
    /**
     * ENHANCED: Diagnose specific error patterns including recovery info
     */
    private static String diagnoseErrorPattern(byte[] data, 
                                             TelemetryFrame.FrameValidationResult result,
                                             boolean wasResynced) {
        
        if (result.isValid() && wasResynced) {
            return "Frame was corrupted but successfully recovered via resynchronization";
        }
        
        if ("VALID".equals(result.overallStatus)) {
            return "No structural errors detected";
        }
        
        switch (result.overallStatus) {
            case "SYNC_LOST":
                return analyzeSyncLossPattern(data);
                
            case "TRUNCATED":
                return analyzeTruncationPattern(data);
                
            case "MALFORMED":
                return "Frame header corrupted or invalid structure";
                
            case "DEGRADED":
                if ("SYNC_WEAK".equals(result.syncStatus)) {
                    return "Minor sync word corruption (1-2 bit errors)";
                } else {
                    return "Multiple minor structural issues";
                }
                
            default:
                return "Unknown frame integrity issue";
        }
    }
    
    /**
     * ENHANCED: Calculate confidence score including recovery factor
     */
    private static double calculateConfidenceScore(TelemetryFrame.FrameValidationResult result, 
                                                 boolean wasResynced, boolean hadStructuralIssues) {
        double score = 1.0;
        
        // Deduct for sync issues
        switch (result.syncStatus) {
            case "SYNC_VALID": break; // No deduction
            case "SYNC_WEAK": score -= 0.1; break;
            case "SYNC_DEGRADED": score -= 0.3; break;
            case "SYNC_LOST": score -= 0.8; break;
        }
        
        // Deduct for structure issues  
        switch (result.structureStatus) {
            case "LENGTH_VALID": break; // No deduction
            case "OVERSIZED": score -= 0.2; break;
            case "TRUNCATED": score -= 0.6; break;
            case "MALFORMED": score -= 0.9; break;
        }
        
        // FIXED: Recovery logic - successful recovery gets bonus, but not perfect score
        if (wasResynced) {
            if (result.isValid()) {
                score = Math.min(0.85, score + 0.3); // Good recovery but not perfect
            } else {
                score = Math.min(0.7, score + 0.2); // Partial recovery
            }
        }
        
        // Penalty for having had structural issues even if recovered
        if (hadStructuralIssues && result.isValid()) {
            score *= 0.9; // Slight penalty for recovered frames
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * Analyze sync loss patterns to determine likely cause
     */
    private static String analyzeSyncLossPattern(byte[] data) {
        if (data == null || data.length < 4) return "Complete sync loss";
        
        // Check if there's any resemblance to sync word
        byte[] syncBytes = java.nio.ByteBuffer.allocate(4).putInt(TelemetryFrame.SYNC_WORD).array();
        int bestMatch = 0;
        
        for (int i = 0; i < Math.min(data.length, 20); i++) {
            int matchCount = 0;
            for (int j = 0; j < 4 && i + j < data.length; j++) {
                if (data[i + j] == syncBytes[j]) matchCount++;
            }
            bestMatch = Math.max(bestMatch, matchCount);
        }
        
        if (bestMatch == 0) return "Complete sync loss - no sync word detected";
        if (bestMatch == 1) return "Severe sync corruption - 1 matching byte";
        if (bestMatch == 2) return "Major sync corruption - 2 matching bytes";
        return "Partial sync corruption - " + bestMatch + " matching bytes";
    }
    
    /**
     * Analyze truncation patterns
     */
    private static String analyzeTruncationPattern(byte[] data) {
        if (data == null) return "Null data";
        
        int minSize = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + TelemetryFrame.CRC_SIZE;
        
        if (data.length < minSize) {
            int missing = minSize - data.length;
            return "Severe truncation - missing " + missing + " bytes of header";
        }
        
        try {
            // Try to read declared payload length
            if (data.length >= TelemetryFrame.POS_PAYLOAD_LENGTH + 2) {
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(
                    data, TelemetryFrame.POS_PAYLOAD_LENGTH, 2);
                int declaredLength = buffer.getShort() & 0xFFFF;
                int expectedSize = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + 
                                 declaredLength + TelemetryFrame.CRC_SIZE;
                
                if (data.length < expectedSize) {
                    int missing = expectedSize - data.length;
                    return "Payload truncation - missing " + missing + " bytes of payload";
                }
            }
        } catch (Exception e) {
            // Continue with basic analysis
        }
        
        return "General frame truncation - incomplete data";
    }
    
    /**
     * Quick validation for real-time processing
     */
    public static boolean quickValidate(byte[] frameData) {
        ValidationReport report = validateCompleteFrame(frameData);
        return "VALID".equals(report.overallStatus) || "RECOVERED".equals(report.overallStatus);
    }
    
    /**
     * ENHANCED: Comprehensive validation report with recovery info
     */
    public static class ValidationReport {
        public final String overallStatus;
        public final String syncStatus;
        public final String errorDiagnosis;
        public final double confidenceScore;
        public final boolean wasResynced;
        public final byte[] correctedData;
        public final boolean hadStructuralIssues;
        public final long timestamp;
        
        public ValidationReport(String overallStatus, String syncStatus, 
                              String errorDiagnosis, double confidenceScore,
                              boolean wasResynced, byte[] correctedData,
                              boolean hadStructuralIssues) {
            this.overallStatus = overallStatus;
            this.syncStatus = syncStatus;
            this.errorDiagnosis = errorDiagnosis;
            this.confidenceScore = confidenceScore;
            this.wasResynced = wasResynced;
            this.correctedData = correctedData;
            this.hadStructuralIssues = hadStructuralIssues;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return "VALID".equals(overallStatus) || "RECOVERED".equals(overallStatus);
        }
        
        public boolean isRecovered() {
            return "RECOVERED".equals(overallStatus);
        }
        
        public boolean isCorrectable() {
            return confidenceScore > 0.5 && !"MALFORMED".equals(overallStatus);
        }
        
        @Override
        public String toString() {
            String base = String.format("FrameValidation[%s] Confidence:%.1f%% Sync:%s Diagnosis:%s",
                                      overallStatus, confidenceScore * 100, syncStatus, errorDiagnosis);
            
            if (wasResynced) {
                base += " 🔄 RESYNCED";
            }
            if (isRecovered()) {
                base += " 🎉 RECOVERED";
            }
            
            return base;
        }
    }
}