package com.aerospace.strive.correction;

import com.aerospace.strive.transmission.CRCErrorDetector;
import com.aerospace.strive.transmission.FrameValidator;
import com.aerospace.strive.transmission.TelemetryFrame;

/**
 * INTELLIGENT ERROR PATTERN ANALYZER
 * Detects and classifies all satellite error types for optimal correction strategy
 * Uses statistical analysis, feature extraction, and machine learning principles
 */
public class ErrorPatternAnalyzer {
    
    // Error type classifications
    public static final String ERROR_SINGLE_BIT = "SINGLE_BIT_FLIP";
    public static final String ERROR_BURST_NOISE = "BURST_NOISE"; 
    public static final String ERROR_HEADER_DRIFT = "HEADER_DRIFT";
    public static final String ERROR_PACKET_TRUNCATION = "PACKET_TRUNCATION";
    public static final String ERROR_RANDOM_NOISE = "RANDOM_NOISE";
    public static final String ERROR_GARBAGE_DATA = "GARBAGE_DATA";
    public static final String ERROR_SEVERE_CORRUPTION = "SEVERE_CORRUPTION";
    public static final String ERROR_NO_ERROR = "NO_ERROR";
    
    // Analysis thresholds (configurable based on satellite channel conditions)
    private static final double BURST_THRESHOLD = 0.7;        // 70% consecutive errors = burst
    private static final double RANDOM_NOISE_THRESHOLD = 0.3; // 30% random errors = noise
    private static final double GARBAGE_THRESHOLD = 0.9;      // 90% corruption = garbage
    private static final int MIN_BURST_LENGTH = 3;            // Minimum burst size
    
    // Performance monitoring
    private long analysisOperations = 0;
    private long singleBitDetections = 0;
    private long burstDetections = 0;
    private long headerDriftDetections = 0;
    private long truncationDetections = 0;
    private long noiseDetections = 0;
    
    public ErrorPatternAnalyzer() {
        System.out.println("🧠 INTELLIGENT ERROR PATTERN ANALYZER INITIALIZED");
        System.out.println("   - Statistical feature extraction");
        System.out.println("   - Multi-dimensional error classification"); 
        System.out.println("   - Satellite-grade error detection");
    }
    
    /**
     * COMPREHENSIVE ERROR PATTERN ANALYSIS
     * Analyzes corrupted data and returns detailed error signature
     */
    public ErrorSignature analyzeErrorPattern(byte[] corruptedData, byte[] originalData) {
        analysisOperations++;
        
        if (corruptedData == null) {
            return new ErrorSignature(ERROR_SEVERE_CORRUPTION, 1.0, "Null data");
        }
        
        // Step 1: Structural integrity analysis
        StructuralAnalysis structural = analyzeStructuralIntegrity(corruptedData);
        
        // Step 2: If we have original data, do bit-level analysis
        BitLevelAnalysis bitAnalysis = null;
        if (originalData != null) {
            bitAnalysis = analyzeBitLevelPattern(corruptedData, originalData);
        }
        
        // Step 3: Combined analysis to determine error type
        return classifyErrorType(structural, bitAnalysis, corruptedData);
    }
    
    /**
     * ANALYZE STRUCTURAL INTEGRITY (Layer 1 + Layer 2 validation)
     * Detects: Header drift, truncation, sync loss, severe corruption
     */
    private StructuralAnalysis analyzeStructuralIntegrity(byte[] data) {
        StructuralAnalysis result = new StructuralAnalysis();
        
        // Use existing validation framework
        FrameValidator.ValidationReport frameReport = FrameValidator.validateCompleteFrame(data);
        CRCErrorDetector.CRCValidationReport crcReport = CRCErrorDetector.validateDataIntegrity(data);
        
        result.frameValid = frameReport.isValid();
        result.crcValid = crcReport.isValid();
        result.wasResynced = frameReport.wasResynced;
        result.structureConfidence = frameReport.confidenceScore;
        result.dataIntegrityConfidence = crcReport.confidence;
        
        // Analyze structural patterns
        result.hasHeaderDrift = detectHeaderDrift(data, frameReport);
        result.isTruncated = detectPacketTruncation(data, frameReport);
        result.isSeverelyCorrupted = detectSevereCorruption(frameReport, crcReport);
        
        return result;
    }
    
    /**
     * ANALYZE BIT-LEVEL ERROR PATTERNS  
     * Detects: Single-bit flips, burst noise, random noise, garbage data
     * Requires original data for comparison
     */
    private BitLevelAnalysis analyzeBitLevelPattern(byte[] corrupted, byte[] original) {
        BitLevelAnalysis result = new BitLevelAnalysis();
        
        if (corrupted.length != original.length) {
            result.sizeMismatch = true;
            return result; // Can't do bit-level analysis on different sizes
        }
        
        int totalBits = corrupted.length * 8;
        int errorBits = 0;
        int currentBurstLength = 0;
        int maxBurstLength = 0;
        boolean inBurst = false;
        
        // Analyze each byte
        for (int i = 0; i < corrupted.length; i++) {
            byte corrByte = corrupted[i];
            byte origByte = original[i];
            
            if (corrByte != origByte) {
                // Byte-level difference found - analyze bit patterns
                int byteDiff = corrByte ^ origByte;
                int byteErrorBits = Integer.bitCount(byteDiff & 0xFF);
                errorBits += byteErrorBits;
                
                // Burst detection
                if (byteErrorBits > 0) {
                    if (!inBurst) {
                        inBurst = true;
                        currentBurstLength = 1;
                    } else {
                        currentBurstLength++;
                    }
                    maxBurstLength = Math.max(maxBurstLength, currentBurstLength);
                } else {
                    inBurst = false;
                    currentBurstLength = 0;
                }
                
                // Pattern analysis
                result.errorBytes++;
                if (byteErrorBits == 1) {
                    result.singleBitErrorBytes++;
                } else if (byteErrorBits == 8) {
                    result.completelyFlippedBytes++;
                }
            } else {
                inBurst = false;
                currentBurstLength = 0;
            }
        }
        
        // Calculate statistics
        result.totalBits = totalBits;
        result.errorBitCount = errorBits;
        result.errorRate = (double) errorBits / totalBits;
        result.maxBurstLength = maxBurstLength;
        result.hasBurstErrors = maxBurstLength >= MIN_BURST_LENGTH;
        result.burstScore = (double) maxBurstLength / corrupted.length;
        
        return result;
    }
    
    /**
     * INTELLIGENT ERROR TYPE CLASSIFICATION
     * Uses multi-dimensional analysis to determine exact error type
     */
    private ErrorSignature classifyErrorType(StructuralAnalysis structural, 
                                           BitLevelAnalysis bitAnalysis, 
                                           byte[] corruptedData) {
        
        // CASE 1: No structural issues, possible data corruption
        if (structural.frameValid && structural.crcValid) {
            return new ErrorSignature(ERROR_NO_ERROR, 0.95, "No errors detected");
        }
        
        // CASE 2: Header drift detection
        if (structural.hasHeaderDrift || structural.wasResynced) {
            headerDriftDetections++;
            double confidence = Math.max(structural.structureConfidence, 0.7);
            return new ErrorSignature(ERROR_HEADER_DRIFT, confidence, 
                                   "Frame synchronization issue detected");
        }
        
        // CASE 3: Packet truncation
        if (structural.isTruncated) {
            truncationDetections++;
            return new ErrorSignature(ERROR_PACKET_TRUNCATION, 0.85, 
                                   "Incomplete frame transmission");
        }
        
        // CASE 4: Severe corruption (unrecoverable)
        if (structural.isSeverelyCorrupted) {
            return new ErrorSignature(ERROR_SEVERE_CORRUPTION, 0.9, 
                                   "Severe structural damage");
        }
        
        // CASE 5: Bit-level analysis available
        if (bitAnalysis != null && !bitAnalysis.sizeMismatch) {
            
            // Single-bit errors (ideal for Hamming)
            if (bitAnalysis.errorBitCount == 1) {
                singleBitDetections++;
                return new ErrorSignature(ERROR_SINGLE_BIT, 0.98, 
                                       "Single-bit flip detected");
            }
            
            // Burst noise (solar flares)
            if (bitAnalysis.hasBurstErrors && bitAnalysis.burstScore > BURST_THRESHOLD) {
                burstDetections++;
                return new ErrorSignature(ERROR_BURST_NOISE, 0.88, 
                                       String.format("Burst error: %d consecutive bytes", 
                                                   bitAnalysis.maxBurstLength));
            }
            
            // Random noise (RF interference)
            if (bitAnalysis.errorRate > RANDOM_NOISE_THRESHOLD && 
                !bitAnalysis.hasBurstErrors) {
                noiseDetections++;
                return new ErrorSignature(ERROR_RANDOM_NOISE, 0.82, 
                                       String.format("Random noise: %.1f%% error rate", 
                                                   bitAnalysis.errorRate * 100));
            }
            
            // Garbage data (complete corruption)
            if (bitAnalysis.errorRate > GARBAGE_THRESHOLD) {
                return new ErrorSignature(ERROR_GARBAGE_DATA, 0.95, 
                                       "Complete data corruption");
            }
        }
        
        // CASE 6: Default - data corruption of unknown type
        if (!structural.crcValid) {
            return new ErrorSignature(ERROR_RANDOM_NOISE, 0.75, 
                                   "Data integrity failure - unknown pattern");
        }
        
        // CASE 7: Fallback
        return new ErrorSignature(ERROR_SEVERE_CORRUPTION, 0.6, 
                               "Unknown error pattern");
    }
    
    /**
     * DETECT HEADER DRIFT using frame validation results
     */
    private boolean detectHeaderDrift(byte[] data, FrameValidator.ValidationReport report) {
        return report.wasResynced || 
               "SYNC_LOST".equals(report.syncStatus) ||
               "SYNC_WEAK".equals(report.syncStatus);
    }
    
    /**
     * DETECT PACKET TRUNCATION
     */
    private boolean detectPacketTruncation(byte[] data, FrameValidator.ValidationReport report) {
        return "TRUNCATED".equals(report.overallStatus) ||
               data.length < (TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + TelemetryFrame.CRC_SIZE);
    }
    
    /**
     * DETECT SEVERE CORRUPTION
     */
    private boolean detectSevereCorruption(FrameValidator.ValidationReport frameReport,
                                         CRCErrorDetector.CRCValidationReport crcReport) {
        return ("SYNC_LOST".equals(frameReport.overallStatus) && 
                !frameReport.wasResynced) ||
               ("MALFORMED".equals(frameReport.overallStatus)) ||
               (frameReport.confidenceScore < 0.2);
    }
    
    /**
     * QUICK ANALYSIS - For real-time satellite processing
     */
    public ErrorSignature quickAnalyze(byte[] corruptedData) {
        return analyzeErrorPattern(corruptedData, null);
    }
    
    /**
     * GET PERFORMANCE STATISTICS
     */
    public AnalysisStatistics getStatistics() {
        return new AnalysisStatistics(
            analysisOperations,
            singleBitDetections,
            burstDetections, 
            headerDriftDetections,
            truncationDetections,
            noiseDetections
        );
    }
    
    /**
     * RESET PERFORMANCE COUNTERS
     */
    public void resetStatistics() {
        analysisOperations = 0;
        singleBitDetections = 0;
        burstDetections = 0;
        headerDriftDetections = 0;
        truncationDetections = 0;
        noiseDetections = 0;
    }
    
    // =========================================================================
    // DATA STRUCTURES FOR ANALYSIS RESULTS
    // =========================================================================
    
    /**
     * Structural integrity analysis results
     */
    private static class StructuralAnalysis {
        boolean frameValid;
        boolean crcValid;
        boolean wasResynced;
        boolean hasHeaderDrift;
        boolean isTruncated;
        boolean isSeverelyCorrupted;
        double structureConfidence;
        double dataIntegrityConfidence;
    }
    
    /**
     * Bit-level pattern analysis results  
     */
    private static class BitLevelAnalysis {
        boolean sizeMismatch = false;
        int totalBits;
        int errorBitCount;
        int errorBytes;
        int singleBitErrorBytes;
        int completelyFlippedBytes;
        double errorRate;
        int maxBurstLength;
        boolean hasBurstErrors;
        double burstScore;
    }
    
    /**
     * COMPREHENSIVE ERROR SIGNATURE
     * Contains error type, confidence, and detailed diagnosis
     */
    public static class ErrorSignature {
        public final String errorType;
        public final double confidence;
        public final String diagnosis;
        public final long timestamp;
        
        public ErrorSignature(String errorType, double confidence, String diagnosis) {
            this.errorType = errorType;
            this.confidence = confidence;
            this.diagnosis = diagnosis;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isCorrectable() {
            return !ERROR_SEVERE_CORRUPTION.equals(errorType) && 
                   !ERROR_GARBAGE_DATA.equals(errorType);
        }
        
        public boolean needsRetransmission() {
            return ERROR_SEVERE_CORRUPTION.equals(errorType) ||
                   ERROR_GARBAGE_DATA.equals(errorType);
        }
        
        @Override
        public String toString() {
            return String.format("ErrorSignature[%s] Confidence:%.1f%% - %s", 
                               errorType, confidence * 100, diagnosis);
        }
    }
    
    /**
     * Performance statistics container
     */
    public static class AnalysisStatistics {
        public final long totalAnalyses;
        public final long singleBitDetections;
        public final long burstDetections;
        public final long headerDriftDetections;
        public final long truncationDetections;
        public final long noiseDetections;
        
        public AnalysisStatistics(long totalAnalyses, long singleBitDetections,
                                long burstDetections, long headerDriftDetections,
                                long truncationDetections, long noiseDetections) {
            this.totalAnalyses = totalAnalyses;
            this.singleBitDetections = singleBitDetections;
            this.burstDetections = burstDetections;
            this.headerDriftDetections = headerDriftDetections;
            this.truncationDetections = truncationDetections;
            this.noiseDetections = noiseDetections;
        }
        
        public double getSingleBitRate() {
            return totalAnalyses > 0 ? (double) singleBitDetections / totalAnalyses : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Analysis Stats: total=%d, single_bit=%d, burst=%d, header_drift=%d, truncation=%d, noise=%d",
                totalAnalyses, singleBitDetections, burstDetections, headerDriftDetections,
                truncationDetections, noiseDetections
            );
        }
    }
}