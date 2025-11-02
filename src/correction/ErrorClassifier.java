package correction;

import correction.ErrorPatternAnalyzer.ErrorAnalysis;

/**
 * INTELLIGENT ERROR CLASSIFICATION ENGINE
 * Routes errors to optimal algorithms based on performance testing
 * Includes safety checks to prevent algorithm degradation
 */
public class ErrorClassifier {
    
    /**
     * Correction algorithms with performance characteristics
     */
    public enum CorrectionAlgorithm {
        // ✅ Working well
        VITERBI_DECODER,     // Sync drift only (proven working)
        BCH_CODEC,           // Random bit errors (safe default)
        
        // 🚧 Planned implementation  
        LDPC_CODEC,          // Gaussian noise
        TURBO_CODEC,         // Severe conditions
        INTERLEAVER_ONLY,    // Burst error protection
        
        // 🔒 Safety modes
        NO_CORRECTION_NEEDED, // Minimal errors
        SAFE_MODE            // Fallback when uncertain
    }
    
    /**
     * Correction strategy with safety features
     */
    public static class CorrectionStrategy {
        public final CorrectionAlgorithm primaryAlgorithm;
        public final CorrectionAlgorithm secondaryAlgorithm;
        public final String configuration;
        public final String rationale;
        public final double confidence;
        
        public CorrectionStrategy(CorrectionAlgorithm primary, 
                                CorrectionAlgorithm secondary, 
                                String config, String rationale, double confidence) {
            this.primaryAlgorithm = primary;
            this.secondaryAlgorithm = secondary;
            this.configuration = config;
            this.rationale = rationale;
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return String.format("STRATEGY: %s%s | %s (Confidence: %.0f%%)",
                primaryAlgorithm,
                secondaryAlgorithm != null ? " + " + secondaryAlgorithm : "",
                configuration, confidence * 100);
        }
        
        public String getDetailedRationale() {
            return String.format("%s -> %s (%.0f%% confidence)\nRationale: %s",
                primaryAlgorithm, configuration, confidence * 100, rationale);
        }
    }
    
    /**
     * Intelligent classification with performance-based routing
     */
    public CorrectionStrategy classifyOptimalStrategy(ErrorAnalysis analysis) {
        // Safety first: No correction for minimal errors
        if (!analysis.requiresCorrection) {
            return new CorrectionStrategy(
                CorrectionAlgorithm.NO_CORRECTION_NEEDED,
                null,
                "No correction",
                "Error density below threshold (" + String.format("%.1f%%", analysis.errorDensity * 100) + ")",
                0.95
            );
        }
        
        // Performance-based routing based on actual testing
        switch (analysis.primaryErrorType) {
            case SYNC_DRIFT:
                return handleSyncDrift(analysis);
                
            case RANDOM_BIT_ERROR:
                return handleRandomBitErrors(analysis);
                
            case BURST_ERROR:
                return handleBurstErrors(analysis);
                
            case GAUSSIAN_NOISE:
                return handleGaussianNoise(analysis);
                
            case PACKET_LOSS:
                return handlePacketLoss(analysis);
                
            case MIXED_ERRORS:
                return handleMixedErrors(analysis);
                
            case MINOR_CORRUPTION:
            default:
                return handleSafeDefault(analysis);
        }
    }
    
    /**
     * Viterbi for sync drift (proven working case)
     */
    private CorrectionStrategy handleSyncDrift(ErrorAnalysis analysis) {
        double confidence = Math.min(0.85, analysis.syncDriftScore * 1.5);
        
        // Only use Viterbi for clear sync patterns
        if (analysis.syncDriftScore > 0.3 && analysis.burstErrorScore < 0.3) {
            return new CorrectionStrategy(
                CorrectionAlgorithm.VITERBI_DECODER,
                null,
                "Viterbi(R1/2,K7) for sync recovery",
                "Strong sync drift pattern detected (score: " + String.format("%.2f", analysis.syncDriftScore) + ")",
                confidence
            );
        } else {
            // Fall back to safe mode for ambiguous cases
            return handleSafeDefault(analysis);
        }
    }
    
    /**
     * BCH for random bit errors (safe default)
     */
    private CorrectionStrategy handleRandomBitErrors(ErrorAnalysis analysis) {
        return new CorrectionStrategy(
            CorrectionAlgorithm.BCH_CODEC,
            null,
            "BCH(15,7,2) for random errors",
            "Random error pattern ideal for BCH correction",
            0.90
        );
    }
    
    /**
     * Conservative approach for burst errors
     */
    private CorrectionStrategy handleBurstErrors(ErrorAnalysis analysis) {
        // Use BCH as safe default (Viterbi makes bursts worse)
        return new CorrectionStrategy(
            CorrectionAlgorithm.BCH_CODEC,
            null,
            "BCH for burst errors (safe mode)",
            "Burst errors detected - using BCH as safe default",
            0.80
        );
    }
    
    /**
     * Placeholder for Gaussian noise
     */
    private CorrectionStrategy handleGaussianNoise(ErrorAnalysis analysis) {
        // Fall back to BCH until LDPC is implemented
        return new CorrectionStrategy(
            CorrectionAlgorithm.BCH_CODEC,
            null,
            "BCH for Gaussian noise (LDPC planned)",
            "Gaussian noise detected - using BCH until LDPC implemented",
            0.70
        );
    }
    
    /**
     * Placeholder for packet loss
     */
    private CorrectionStrategy handlePacketLoss(ErrorAnalysis analysis) {
        // Fall back to safe mode until Turbo is implemented
        return new CorrectionStrategy(
            CorrectionAlgorithm.SAFE_MODE,
            null,
            "Safe mode for packet loss (Turbo planned)",
            "Packet loss detected - safe mode until Turbo codes implemented",
            0.65
        );
    }
    
    /**
     * Conservative approach for mixed errors
     */
    private CorrectionStrategy handleMixedErrors(ErrorAnalysis analysis) {
        // Analyze the dominant component
        double maxScore = Math.max(analysis.syncDriftScore, 
                                  Math.max(analysis.randomErrorScore, 
                                          Math.max(analysis.burstErrorScore, 
                                                  analysis.gaussianNoiseScore)));
        
        if (analysis.syncDriftScore == maxScore && analysis.syncDriftScore > 0.25) {
            return handleSyncDrift(analysis);
        } else if (analysis.randomErrorScore == maxScore && analysis.randomErrorScore > 0.2) {
            return handleRandomBitErrors(analysis);
        } else {
            // Default to safe mode for ambiguous mixed errors
            return handleSafeDefault(analysis);
        }
    }
    
    /**
     * Safe default strategy when uncertain
     */
    private CorrectionStrategy handleSafeDefault(ErrorAnalysis analysis) {
        return new CorrectionStrategy(
            CorrectionAlgorithm.BCH_CODEC,
            null,
            "BCH safe default",
            "Uncertain error pattern - using BCH as safe default",
            0.75
        );
    }
    
    /**
     * Get algorithm performance characteristics
     */
    public static String getAlgorithmInfo(CorrectionAlgorithm algorithm) {
        switch (algorithm) {
            case VITERBI_DECODER:
                return "Viterbi: Rate 1/2, K=7. Best for sync drift. Avoid for random/burst errors.";
            case BCH_CODEC:
                return "BCH: (15,7,2) code. Safe default for most error types. Guaranteed 2-bit correction.";
            case LDPC_CODEC:
                return "LDPC: Near-Shannon performance for Gaussian noise. (Planned)";
            case TURBO_CODEC:
                return "Turbo: Excellent for severe conditions and packet loss. (Planned)";
            case INTERLEAVER_ONLY:
                return "Interleaver: Spreads burst errors for easier correction. (Planned)";
            case NO_CORRECTION_NEEDED:
                return "No Correction: For minimal errors (<1% density)";
            case SAFE_MODE:
                return "Safe Mode: Conservative approach when uncertain";
            default:
                return "Unknown algorithm";
        }
    }
}