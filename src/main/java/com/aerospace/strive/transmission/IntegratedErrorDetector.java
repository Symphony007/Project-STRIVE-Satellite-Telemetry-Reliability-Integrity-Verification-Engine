package com.aerospace.strive.transmission;

/**
 * INTEGRATED ERROR DETECTION PIPELINE - COMPLETE UPDATED VERSION
 * Combines Layer 1 (Structural) and Layer 2 (CRC) validation
 * FIXED: Proper handling of recovered frames and all error scenarios
 */
public class IntegratedErrorDetector {
    
    /**
     * Complete error detection pipeline - ENHANCED for recovery detection
     */
    public static ComprehensiveErrorReport detectAllErrors(byte[] frameData) {
        if (frameData == null) {
            return new ComprehensiveErrorReport("INVALID", "Null frame data", 0.0, "NULL_DATA");
        }
        
        // LAYER 1: Structural Integrity Check
        FrameValidator.ValidationReport layer1Report = FrameValidator.validateCompleteFrame(frameData);
        
        // FIXED: Handle recovered frames specially
        CRCErrorDetector.CRCValidationReport layer2Report = null;
        boolean canPerformCRC = canPerformCRCCheck(frameData, layer1Report);
        
        if (canPerformCRC) {
            layer2Report = CRCErrorDetector.validateDataIntegrity(
                layer1Report.correctedData != null ? layer1Report.correctedData : frameData
            );
        }
        
        // Combine results with enhanced logic for recovered frames
        return combineValidationResults(layer1Report, layer2Report, canPerformCRC);
    }
    
    /**
     * Determine if CRC check is possible
     */
    private static boolean canPerformCRCCheck(byte[] frameData, FrameValidator.ValidationReport layer1) {
        if (frameData == null) return false;
        
        // Use corrected data if available
        byte[] dataToCheck = layer1.correctedData != null ? layer1.correctedData : frameData;
        
        // Can perform CRC if we have at least sync + header + CRC
        int minSizeForCRC = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + TelemetryFrame.CRC_SIZE;
        if (dataToCheck.length < minSizeForCRC) return false;
        
        // Can perform CRC if structure is somewhat intact
        return layer1.confidenceScore > 0.2;
    }
    
    /**
     * ENHANCED: Intelligent combination accounting for recovery
     */
    private static ComprehensiveErrorReport combineValidationResults(
            FrameValidator.ValidationReport layer1, 
            CRCErrorDetector.CRCValidationReport layer2,
            boolean crcWasAttempted) {
        
        String overallStatus;
        String detailedDiagnosis;
        double combinedConfidence;
        String errorType;
        
        // CASE 1: Genuinely valid frame (no issues ever)
        if (layer1.overallStatus.equals("VALID") && 
            (layer2 == null || layer2.isValid()) &&
            !layer1.hadStructuralIssues && !layer1.wasResynced) {
            overallStatus = "VALID";
            detailedDiagnosis = "Frame structure and data integrity verified";
            combinedConfidence = 0.95;
            errorType = "NO_ERROR";
        }
        // CASE 2: Recovered frame (had issues but fixed)
        else if (layer1.isValid() && layer1.isRecovered()) {
            overallStatus = "RECOVERED";
            detailedDiagnosis = layer1.errorDiagnosis;
            combinedConfidence = layer1.confidenceScore;
            errorType = "RECOVERED_" + classifyStructuralError(layer1);
            
            // Add CRC info if available
            if (layer2 != null) {
                if (layer2.isValid()) {
                    detailedDiagnosis += " + Data integrity maintained after recovery";
                    combinedConfidence = Math.min(combinedConfidence + 0.1, 0.9);
                } else {
                    detailedDiagnosis += " + Data corruption remains: " + layer2.diagnosis;
                    combinedConfidence = Math.min(combinedConfidence, layer2.confidence);
                    errorType += "_WITH_DATA_CORRUPTION";
                }
            }
        }
        // CASE 3: Layer 1 failed with severe structural issues
        else if (layer1.overallStatus.equals("SYNC_LOST") || 
                 layer1.overallStatus.equals("MALFORMED") ||
                 layer1.confidenceScore < 0.3) {
            overallStatus = layer1.overallStatus;
            detailedDiagnosis = "Severe structural issue: " + layer1.errorDiagnosis;
            combinedConfidence = layer1.confidenceScore;
            errorType = classifyStructuralError(layer1);
            
            // Add CRC info if available
            if (layer2 != null && !layer2.isValid()) {
                detailedDiagnosis += " + Data corruption: " + layer2.diagnosis;
                combinedConfidence = Math.min(combinedConfidence, layer2.confidence);
            } else if (!crcWasAttempted) {
                detailedDiagnosis += " (CRC check not possible due to structural damage)";
            }
        }
        // CASE 4: Layer 1 passed but Layer 2 failed (hidden data corruption)
        else if (layer1.isValid() && layer2 != null && !layer2.isValid()) {
            overallStatus = "DATA_CORRUPTED";
            detailedDiagnosis = "Structure valid but data corrupted: " + layer2.diagnosis;
            combinedConfidence = layer2.confidence;
            errorType = "DATA_CORRUPTION";
        }
        // CASE 5: Layer 1 has minor issues but structure is mostly intact
        else if (!layer1.isValid() && layer1.confidenceScore >= 0.5) {
            overallStatus = "DEGRADED";
            detailedDiagnosis = "Minor structural issues: " + layer1.errorDiagnosis;
            combinedConfidence = layer1.confidenceScore;
            errorType = "MINOR_STRUCTURAL_ERROR";
            
            // Add CRC info
            if (layer2 != null && !layer2.isValid()) {
                detailedDiagnosis += " + Data corruption: " + layer2.diagnosis;
                combinedConfidence = Math.min(combinedConfidence, layer2.confidence);
            }
        }
        // CASE 6: Default case
        else {
            overallStatus = layer1.overallStatus;
            detailedDiagnosis = layer1.errorDiagnosis;
            combinedConfidence = layer1.confidenceScore;
            errorType = "UNKNOWN_ERROR";
            
            if (layer2 != null && !layer2.isValid()) {
                detailedDiagnosis += " + " + layer2.diagnosis;
            }
        }
        
        return new ComprehensiveErrorReport(overallStatus, detailedDiagnosis, combinedConfidence, errorType);
    }
    
    /**
     * Classify structural errors for better reporting
     */
    private static String classifyStructuralError(FrameValidator.ValidationReport layer1) {
        if (layer1.overallStatus.equals("SYNC_LOST")) {
            return "SYNC_ERROR";
        } else if (layer1.overallStatus.equals("TRUNCATED")) {
            return "TRUNCATION_ERROR";
        } else if (layer1.overallStatus.equals("MALFORMED")) {
            return "STRUCTURAL_ERROR";
        } else if (layer1.overallStatus.equals("DEGRADED")) {
            return "MINOR_STRUCTURAL_ERROR";
        } else if (layer1.isRecovered()) {
            return "RECOVERED_ERROR";
        }
        return "STRUCTURAL_ERROR";
    }
    
    /**
     * Get error type classification for reporting
     */
    public static String classifyErrorType(FrameValidator.ValidationReport layer1,
                                         CRCErrorDetector.CRCValidationReport layer2) {
        
        if (layer1.isValid() && (layer2 == null || layer2.isValid())) {
            return "NO_ERROR";
        }
        
        if (!layer1.isValid()) {
            if (layer1.overallStatus.equals("SYNC_LOST")) return "SYNC_ERROR";
            if (layer1.overallStatus.equals("TRUNCATED")) return "TRUNCATION_ERROR";
            if (layer1.overallStatus.equals("MALFORMED")) return "STRUCTURAL_ERROR";
            return "FRAME_ERROR";
        }
        
        if (layer2 != null && !layer2.isValid()) {
            return "DATA_CORRUPTION";
        }
        
        return "UNKNOWN_ERROR";
    }
    
    /**
     * Enhanced error detection with detailed analysis
     */
    public static DetailedErrorReport analyzeErrorsInDetail(byte[] frameData) {
        ComprehensiveErrorReport basicReport = detectAllErrors(frameData);
        
        // Additional analysis
        String severity = calculateErrorSeverity(basicReport);
        String recommendedAction = getRecommendedAction(basicReport);
        boolean needsRetransmission = basicReport.needsRetransmission();
        boolean canBeCorrected = basicReport.canAttemptCorrection();
        
        return new DetailedErrorReport(
            basicReport.overallStatus,
            basicReport.detailedDiagnosis,
            basicReport.confidence,
            basicReport.errorType,
            severity,
            recommendedAction,
            needsRetransmission,
            canBeCorrected
        );
    }
    
    /**
     * Calculate error severity based on confidence and type
     */
    private static String calculateErrorSeverity(ComprehensiveErrorReport report) {
        if (report.isValid()) return "NONE";
        
        if (report.confidence < 0.3) return "CRITICAL";
        if (report.confidence < 0.6) return "SEVERE";
        if (report.confidence < 0.8) return "MODERATE";
        return "MINOR";
    }
    
    /**
     * Get recommended action based on error type and severity
     */
    private static String getRecommendedAction(ComprehensiveErrorReport report) {
        if (report.isValid()) return "PROCESS_DATA";
        
        switch (report.errorType) {
            case "SYNC_ERROR":
                return "ATTEMPT_RESYNC_OR_RETRANSMIT";
            case "TRUNCATION_ERROR":
                return report.confidence > 0.5 ? "ATTEMPT_RECOVERY" : "REQUEST_RETRANSMISSION";
            case "DATA_CORRUPTION":
                return "USE_ERROR_CORRECTION_OR_RETRANSMIT";
            case "MINOR_STRUCTURAL_ERROR":
                return "ATTEMPT_CORRECTION";
            case "RECOVERED_ERROR":
                return "PROCESS_WITH_CAUTION";
            case "RECOVERED_ERROR_WITH_DATA_CORRUPTION":
                return "USE_ERROR_CORRECTION";
            default:
                return "REQUEST_RETRANSMISSION";
        }
    }
    
    /**
     * Quick validation for real-time processing
     */
    public static boolean quickValidate(byte[] frameData) {
        ComprehensiveErrorReport report = detectAllErrors(frameData);
        return report.isValid() || "RECOVERED".equals(report.overallStatus);
    }
    
    /**
     * Get detailed statistics about error detection performance
     */
    public static DetectionStatistics getDetectionStatistics(byte[][] testFrames) {
        if (testFrames == null || testFrames.length == 0) {
            return new DetectionStatistics(0, 0, 0, 0, 0, 0);
        }
        
        int totalFrames = testFrames.length;
        int validFrames = 0;
        int recoveredFrames = 0;
        int corruptedFrames = 0;
        int undetectedFrames = 0;
        double totalConfidence = 0;
        
        for (byte[] frame : testFrames) {
            ComprehensiveErrorReport report = detectAllErrors(frame);
            totalConfidence += report.confidence;
            
            if (report.isValid()) {
                if ("RECOVERED".equals(report.overallStatus)) {
                    recoveredFrames++;
                } else {
                    validFrames++;
                }
            } else {
                corruptedFrames++;
            }
            
            // Count undetected (false negatives)
            if (report.confidence < 0.1 && !report.overallStatus.equals("INVALID")) {
                undetectedFrames++;
            }
        }
        
        double averageConfidence = totalConfidence / totalFrames;
        double detectionRate = (double) (totalFrames - undetectedFrames) / totalFrames * 100;
        
        return new DetectionStatistics(
            totalFrames, validFrames, recoveredFrames, corruptedFrames, 
            undetectedFrames, detectionRate, averageConfidence
        );
    }
    
    /**
     * Comprehensive Error Report Container
     */
    public static class ComprehensiveErrorReport {
        public final String overallStatus;
        public final String detailedDiagnosis;
        public final double confidence;
        public final long timestamp;
        public final String errorType;
        
        public ComprehensiveErrorReport(String overallStatus, String detailedDiagnosis, 
                                      double confidence, String errorType) {
            this.overallStatus = overallStatus;
            this.detailedDiagnosis = detailedDiagnosis;
            this.confidence = confidence;
            this.timestamp = System.currentTimeMillis();
            this.errorType = errorType;
        }
        
        public boolean isValid() {
            return "VALID".equals(overallStatus) || "RECOVERED".equals(overallStatus);
        }
        
        public boolean isRecovered() {
            return "RECOVERED".equals(overallStatus);
        }
        
        public boolean needsRetransmission() {
            return !isValid() && confidence < 0.5;
        }
        
        public boolean canAttemptCorrection() {
            return !isValid() && confidence >= 0.3;
        }
        
        @Override
        public String toString() {
            return String.format("ErrorReport[%s] Confidence:%.1f%% Type:%s %s", 
                               overallStatus, confidence * 100, errorType, detailedDiagnosis);
        }
    }
    
    /**
     * Detailed Error Report with additional analysis
     */
    public static class DetailedErrorReport {
        public final String overallStatus;
        public final String detailedDiagnosis;
        public final double confidence;
        public final String errorType;
        public final String severity;
        public final String recommendedAction;
        public final boolean needsRetransmission;
        public final boolean canBeCorrected;
        public final long timestamp;
        
        public DetailedErrorReport(String overallStatus, String detailedDiagnosis,
                                 double confidence, String errorType, String severity,
                                 String recommendedAction, boolean needsRetransmission,
                                 boolean canBeCorrected) {
            this.overallStatus = overallStatus;
            this.detailedDiagnosis = detailedDiagnosis;
            this.confidence = confidence;
            this.errorType = errorType;
            this.severity = severity;
            this.recommendedAction = recommendedAction;
            this.needsRetransmission = needsRetransmission;
            this.canBeCorrected = canBeCorrected;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return "VALID".equals(overallStatus) || "RECOVERED".equals(overallStatus);
        }
        
        @Override
        public String toString() {
            return String.format("DetailedErrorReport[%s] Severity:%s Confidence:%.1f%%\n" +
                               "  Type: %s\n  Diagnosis: %s\n  Action: %s\n  Retransmit: %s Correctable: %s",
                               overallStatus, severity, confidence * 100, errorType,
                               detailedDiagnosis, recommendedAction, needsRetransmission, canBeCorrected);
        }
    }
    
    /**
     * Detection Statistics Container
     */
    public static class DetectionStatistics {
        public final int totalFrames;
        public final int validFrames;
        public final int recoveredFrames;
        public final int corruptedFrames;
        public final int undetectedFrames;
        public final double detectionRate;
        public final double averageConfidence;
        public final long timestamp;
        
        public DetectionStatistics(int totalFrames, int validFrames, int recoveredFrames,
                                 int corruptedFrames, int undetectedFrames, double detectionRate,
                                 double averageConfidence) {
            this.totalFrames = totalFrames;
            this.validFrames = validFrames;
            this.recoveredFrames = recoveredFrames;
            this.corruptedFrames = corruptedFrames;
            this.undetectedFrames = undetectedFrames;
            this.detectionRate = detectionRate;
            this.averageConfidence = averageConfidence;
            this.timestamp = System.currentTimeMillis();
        }
        
        public DetectionStatistics(int totalFrames, int validFrames, int recoveredFrames,
                                 int corruptedFrames, int undetectedFrames, double detectionRate) {
            this(totalFrames, validFrames, recoveredFrames, corruptedFrames, undetectedFrames, 
                 detectionRate, 0.0);
        }
        
        public double getErrorRate() {
            return (double) corruptedFrames / totalFrames * 100;
        }
        
        public double getRecoveryRate() {
            return (double) recoveredFrames / (recoveredFrames + corruptedFrames) * 100;
        }
        
        @Override
        public String toString() {
            return String.format(
                "DetectionStats[Total:%d Valid:%d Recovered:%d Corrupted:%d Undetected:%d\n" +
                "  Detection Rate: %.1f%% Error Rate: %.1f%% Recovery Rate: %.1f%% Avg Confidence: %.1f%%]",
                totalFrames, validFrames, recoveredFrames, corruptedFrames, undetectedFrames,
                detectionRate, getErrorRate(), getRecoveryRate(), averageConfidence * 100
            );
        }
    }
}