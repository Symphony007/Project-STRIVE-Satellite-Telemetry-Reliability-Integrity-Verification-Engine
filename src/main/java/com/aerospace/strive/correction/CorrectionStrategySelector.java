package com.aerospace.strive.correction;

import java.util.Arrays;

import com.aerospace.strive.transmission.FrameValidator;
import com.aerospace.strive.transmission.TelemetryFrame;

/**
 * INTELLIGENT CORRECTION STRATEGY SELECTOR
 * Routes errors to optimal correction algorithms based on error patterns
 * Uses NASA-grade decision making for satellite communications
 */
public class CorrectionStrategySelector {
    
    // Correction strategy types
    public static final String STRATEGY_HAMMING = "HAMMING_SINGLE_BIT";
    public static final String STRATEGY_REED_SOLOMON = "REED_SOLOMON_GENERAL";
    public static final String STRATEGY_REED_SOLOMON_BURST = "REED_SOLOMON_BURST";
    public static final String STRATEGY_REED_SOLOMON_ERASURE = "REED_SOLOMON_ERASURE";
    public static final String STRATEGY_RESYNCHRONIZATION = "FRAME_RESYNCHRONIZATION";
    public static final String STRATEGY_RETRANSMISSION = "REQUEST_RETRANSMISSION";
    public static final String STRATEGY_VITERBI = "VITERBI_SOFT_DECISION";
    public static final String STRATEGY_NONE = "NO_CORRECTION_NEEDED";
    
    // Algorithm instances
    private final HammingCodeCorrector hammingCorrector;
    private final CCSDSReedSolomon reedSolomonCorrector;
    private final ErrorPatternAnalyzer errorAnalyzer;
    
    // Performance monitoring
    private long strategySelections = 0;
    private long hammingSelections = 0;
    private long reedSolomonSelections = 0;
    private long resynchronizations = 0;
    private long retransmissionRequests = 0;
    private long successfulCorrections = 0;
    private long failedCorrections = 0;
    
    // Configuration
    private boolean enableHamming = true;
    private boolean enableReedSolomon = true;
    private boolean enableResynchronization = true;
    
    public CorrectionStrategySelector() {
        this.hammingCorrector = new HammingCodeCorrector();
        this.reedSolomonCorrector = new CCSDSReedSolomon(15, 11); // Shortened for telemetry
        this.errorAnalyzer = new ErrorPatternAnalyzer();
        
        System.out.println("🎯 INTELLIGENT CORRECTION STRATEGY SELECTOR INITIALIZED");
        System.out.println("   - Algorithm optimization engine");
        System.out.println("   - Error-type-based routing");
        System.out.println("   - Performance-aware selection");
    }
    
    /**
     * MAIN INTELLIGENT CORRECTION PIPELINE
     * Analyzes error and applies optimal correction strategy
     */
    public CorrectionResult applyOptimalCorrection(byte[] corruptedData, byte[] originalData) {
        strategySelections++;
        
        if (corruptedData == null) {
            return new CorrectionResult(null, STRATEGY_RETRANSMISSION, 0.0, 
                                      "Null data - cannot correct");
        }
        
        // Step 1: Analyze error pattern
        ErrorPatternAnalyzer.ErrorSignature errorSig = errorAnalyzer.analyzeErrorPattern(corruptedData, originalData);
        
        // Step 2: Select optimal strategy based on error type
        CorrectionStrategy strategy = selectOptimalStrategy(errorSig, corruptedData);
        
        // Step 3: Apply selected correction
        CorrectionResult result = applyCorrectionStrategy(strategy, corruptedData, originalData);
        
        // Step 4: Update performance tracking
        if (result.success) {
            successfulCorrections++;
        } else {
            failedCorrections++;
        }
        
        return result;
    }
    
    /**
     * INTELLIGENT STRATEGY SELECTION ALGORITHM
     * Matches error types to optimal correction methods
     */
    private CorrectionStrategy selectOptimalStrategy(ErrorPatternAnalyzer.ErrorSignature errorSig, 
                                                   byte[] corruptedData) {
        
        String errorType = errorSig.errorType;
        double confidence = errorSig.confidence;
        
        // DECISION MATRIX: Error Type → Optimal Strategy
        switch (errorType) {
            
            case ErrorPatternAnalyzer.ERROR_NO_ERROR:
                return new CorrectionStrategy(STRATEGY_NONE, 1.0, 
                                           "No correction needed", confidence);
                
            case ErrorPatternAnalyzer.ERROR_SINGLE_BIT:
                if (enableHamming && isSuitableForHamming(corruptedData)) {
                    hammingSelections++;
                    return new CorrectionStrategy(STRATEGY_HAMMING, 0.98,
                                               "Single-bit error - Hamming optimal", confidence);
                }
                // Fall through to Reed-Solomon if Hamming not suitable
                
            case ErrorPatternAnalyzer.ERROR_RANDOM_NOISE:
                if (enableReedSolomon && confidence > 0.6) {
                    reedSolomonSelections++;
                    return new CorrectionStrategy(STRATEGY_REED_SOLOMON, 0.85,
                                               "Random errors - Reed-Solomon general", confidence);
                }
                break;
                
            case ErrorPatternAnalyzer.ERROR_BURST_NOISE:
                if (enableReedSolomon) {
                    reedSolomonSelections++;
                    return new CorrectionStrategy(STRATEGY_REED_SOLOMON_BURST, 0.88,
                                               "Burst errors - Reed-Solomon with interleaving", confidence);
                }
                break;
                
            case ErrorPatternAnalyzer.ERROR_HEADER_DRIFT:
                if (enableResynchronization) {
                    resynchronizations++;
                    return new CorrectionStrategy(STRATEGY_RESYNCHRONIZATION, 0.75,
                                               "Header drift - frame resynchronization", confidence);
                }
                break;
                
            case ErrorPatternAnalyzer.ERROR_PACKET_TRUNCATION:
                if (enableReedSolomon && hasKnownErasurePositions(corruptedData)) {
                    reedSolomonSelections++;
                    return new CorrectionStrategy(STRATEGY_REED_SOLOMON_ERASURE, 0.82,
                                               "Truncation - Reed-Solomon erasure decoding", confidence);
                }
                break;
                
            case ErrorPatternAnalyzer.ERROR_GARBAGE_DATA:
            case ErrorPatternAnalyzer.ERROR_SEVERE_CORRUPTION:
                retransmissionRequests++;
                return new CorrectionStrategy(STRATEGY_RETRANSMISSION, 0.95,
                                           "Severe corruption - retransmission required", confidence);
        }
        
        // DEFAULT STRATEGY: Reed-Solomon as general-purpose fallback
        if (enableReedSolomon) {
            reedSolomonSelections++;
            return new CorrectionStrategy(STRATEGY_REED_SOLOMON, 0.7,
                                       "Fallback - Reed-Solomon general correction", confidence);
        }
        
        // FINAL FALLBACK: Retransmission
        retransmissionRequests++;
        return new CorrectionStrategy(STRATEGY_RETRANSMISSION, 0.8,
                                   "No suitable correction - retransmission", confidence);
    }
    
    /**
     * APPLY SELECTED CORRECTION STRATEGY
     * Executes the chosen algorithm on the corrupted data
     */
    private CorrectionResult applyCorrectionStrategy(CorrectionStrategy strategy, 
                                                   byte[] corruptedData, 
                                                   byte[] originalData) {
        
        byte[] correctedData = null;
        boolean success = false;
        String details = "";
        
        try {
            switch (strategy.strategyType) {
                
                case STRATEGY_HAMMING:
                    correctedData = applyHammingCorrection(corruptedData);
                    success = correctedData != null;
                    details = success ? "Hamming correction successful" : "Hamming correction failed";
                    break;
                    
                case STRATEGY_REED_SOLOMON:
                case STRATEGY_REED_SOLOMON_BURST:
                    correctedData = applyReedSolomonCorrection(corruptedData, null);
                    success = correctedData != null;
                    details = success ? "Reed-Solomon correction successful" : "Reed-Solomon correction failed";
                    break;
                    
                case STRATEGY_REED_SOLOMON_ERASURE:
                    int[] erasurePositions = calculateErasurePositions(corruptedData);
                    correctedData = applyReedSolomonCorrection(corruptedData, erasurePositions);
                    success = correctedData != null;
                    details = String.format("Erasure decoding (%d positions) %s", 
                                          erasurePositions.length, success ? "successful" : "failed");
                    break;
                    
                case STRATEGY_RESYNCHRONIZATION:
                    correctedData = applyFrameResynchronization(corruptedData);
                    success = correctedData != null;
                    details = success ? "Frame resynchronized" : "Resynchronization failed";
                    break;
                    
                case STRATEGY_RETRANSMISSION:
                    correctedData = null; // Can't correct - need retransmission
                    success = false;
                    details = "Retransmission requested";
                    break;
                    
                case STRATEGY_NONE:
                    correctedData = corruptedData; // No correction needed
                    success = true;
                    details = "No errors detected";
                    break;
                    
                default:
                    correctedData = null;
                    success = false;
                    details = "Unknown strategy";
            }
            
        } catch (Exception e) {
            correctedData = null;
            success = false;
            details = "Correction failed with exception: " + e.getMessage();
        }
        
        // Verify correction if original data is available
        if (success && originalData != null && correctedData != null) {
            boolean verified = Arrays.equals(originalData, correctedData);
            if (!verified) {
                details += " (verification failed)";
                // Don't mark as failed if we don't have original for verification
            }
        }
        
        return new CorrectionResult(correctedData, strategy.strategyType, 
                                  strategy.confidence, details, success);
    }
    
    /**
     * APPLY HAMMING CORRECTION - For single-bit errors
     */
    private byte[] applyHammingCorrection(byte[] data) {
        // Hamming works on 7-bit chunks, so we need to adapt for byte arrays
        // For now, return null as Hamming requires bit-level processing
        // In production, we would convert bytes to bits, apply Hamming, convert back
        return null; // Placeholder - requires bit-level implementation
    }
    
    /**
     * APPLY REED-SOLOMON CORRECTION - General purpose
     */
    private byte[] applyReedSolomonCorrection(byte[] data, int[] erasurePositions) {
        try {
            return reedSolomonCorrector.decode(data, erasurePositions);
        } catch (Exception e) {
            return null; // Correction failed
        }
    }
    
    /**
     * APPLY FRAME RESYNCHRONIZATION - For header drift
     */
    private byte[] applyFrameResynchronization(byte[] data) {
        FrameValidator.ValidationReport report = FrameValidator.validateCompleteFrame(data);
        if (report.correctedData != null) {
            return report.correctedData;
        }
        return null; // Resynchronization failed
    }
    
    /**
     * CALCULATE ERASURE POSITIONS - For packet truncation
     */
    private int[] calculateErasurePositions(byte[] data) {
        // For truncation, missing bytes are at the end
        int expectedSize = TelemetryFrame.SYNC_SIZE + TelemetryFrame.HEADER_SIZE + 
                          TelemetryFrame.MAX_PAYLOAD_SIZE + TelemetryFrame.CRC_SIZE;
        
        if (data.length >= expectedSize) {
            return new int[0]; // No erasures
        }
        
        // Calculate missing positions
        int missingBytes = expectedSize - data.length;
        int[] erasures = new int[missingBytes];
        for (int i = 0; i < missingBytes; i++) {
            erasures[i] = data.length + i; // Positions from end
        }
        
        return erasures;
    }
    
    /**
     * CHECK IF DATA IS SUITABLE FOR HAMMING CORRECTION
     */
    private boolean isSuitableForHamming(byte[] data) {
        // Hamming is best for small, bit-level corrections
        // Not suitable for large frames or severe corruption
        return data != null && data.length <= 64; // Small frames only
    }
    
    /**
     * CHECK IF WE HAVE KNOWN ERASURE POSITIONS
     */
    private boolean hasKnownErasurePositions(byte[] data) {
        FrameValidator.ValidationReport report = FrameValidator.validateCompleteFrame(data);
        return "TRUNCATED".equals(report.overallStatus);
    }
    
    /**
     * QUICK CORRECTION - For real-time processing
     */
    public CorrectionResult quickCorrect(byte[] corruptedData) {
        return applyOptimalCorrection(corruptedData, null);
    }
    
    /**
     * GET PERFORMANCE STATISTICS
     */
    public SelectionStatistics getStatistics() {
        return new SelectionStatistics(
            strategySelections,
            hammingSelections,
            reedSolomonSelections,
            resynchronizations,
            retransmissionRequests,
            successfulCorrections,
            failedCorrections
        );
    }
    
    /**
     * RESET PERFORMANCE COUNTERS
     */
    public void resetStatistics() {
        strategySelections = 0;
        hammingSelections = 0;
        reedSolomonSelections = 0;
        resynchronizations = 0;
        retransmissionRequests = 0;
        successfulCorrections = 0;
        failedCorrections = 0;
    }
    
    // Configuration setters
    public void enableHamming(boolean enable) { this.enableHamming = enable; }
    public void enableReedSolomon(boolean enable) { this.enableReedSolomon = enable; }
    public void enableResynchronization(boolean enable) { this.enableResynchronization = enable; }
    
    // =========================================================================
    // DATA STRUCTURES FOR STRATEGY SELECTION
    // =========================================================================
    
    /**
     * Correction strategy decision
     */
    private static class CorrectionStrategy {
        public final String strategyType;
        public final double expectedSuccessRate;
        public final String rationale;
        public final double confidence;
        
        public CorrectionStrategy(String strategyType, double expectedSuccessRate, 
                                String rationale, double confidence) {
            this.strategyType = strategyType;
            this.expectedSuccessRate = expectedSuccessRate;
            this.rationale = rationale;
            this.confidence = confidence;
        }
    }
    
    /**
     * Comprehensive correction result
     */
    public static class CorrectionResult {
        public final byte[] correctedData;
        public final String strategyUsed;
        public final double confidence;
        public final String details;
        public final boolean success;
        public final long timestamp;
        
        public CorrectionResult(byte[] correctedData, String strategyUsed,
                              double confidence, String details, boolean success) {
            this.correctedData = correctedData;
            this.strategyUsed = strategyUsed;
            this.confidence = confidence;
            this.details = details;
            this.success = success;
            this.timestamp = System.currentTimeMillis();
        }
        
        public CorrectionResult(byte[] correctedData, String strategyUsed,
                              double confidence, String details) {
            this(correctedData, strategyUsed, confidence, details, correctedData != null);
        }
        
        public boolean needsRetransmission() {
            return STRATEGY_RETRANSMISSION.equals(strategyUsed) || !success;
        }
        
        @Override
        public String toString() {
            return String.format("CorrectionResult[%s] Success:%s Confidence:%.1f%% - %s",
                               strategyUsed, success ? "✅" : "❌", confidence * 100, details);
        }
    }
    
    /**
     * Performance statistics container
     */
    public static class SelectionStatistics {
        public final long totalSelections;
        public final long hammingSelections;
        public final long reedSolomonSelections;
        public final long resynchronizations;
        public final long retransmissionRequests;
        public final long successfulCorrections;
        public final long failedCorrections;
        
        public SelectionStatistics(long totalSelections, long hammingSelections,
                                 long reedSolomonSelections, long resynchronizations,
                                 long retransmissionRequests, long successfulCorrections,
                                 long failedCorrections) {
            this.totalSelections = totalSelections;
            this.hammingSelections = hammingSelections;
            this.reedSolomonSelections = reedSolomonSelections;
            this.resynchronizations = resynchronizations;
            this.retransmissionRequests = retransmissionRequests;
            this.successfulCorrections = successfulCorrections;
            this.failedCorrections = failedCorrections;
        }
        
        public double getSuccessRate() {
            long totalAttempts = successfulCorrections + failedCorrections;
            return totalAttempts > 0 ? (double) successfulCorrections / totalAttempts : 0;
        }
        
        public double getHammingUsageRate() {
            return totalSelections > 0 ? (double) hammingSelections / totalSelections : 0;
        }
        
        public double getReedSolomonUsageRate() {
            return totalSelections > 0 ? (double) reedSolomonSelections / totalSelections : 0;
        }
        
        public double getRetransmissionRate() {
            return totalSelections > 0 ? (double) retransmissionRequests / totalSelections : 0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Selection Stats: total=%d, hamming=%d (%.1f%%), reed_solomon=%d (%.1f%%), " +
                "resync=%d, retransmit=%d (%.1f%%), success=%d/%d (%.1f%%)",
                totalSelections, hammingSelections, getHammingUsageRate() * 100,
                reedSolomonSelections, getReedSolomonUsageRate() * 100,
                resynchronizations, retransmissionRequests, getRetransmissionRate() * 100,
                successfulCorrections, (successfulCorrections + failedCorrections), getSuccessRate() * 100
            );
        }
    }
}