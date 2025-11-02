package correction;

import correction.ErrorPatternAnalyzer.ErrorAnalysis;

/**
 * NASA-GRADE ERROR CLASSIFICATION ENGINE
 * Intelligently selects optimal correction algorithms based on error pattern analysis
 * Implements satellite communication best practices for error correction routing
 */
public class ErrorClassifier {
    
    /**
     * Recommended correction algorithms for satellite communications
     */
    public enum CorrectionAlgorithm {
        VITERBI_DECODER,     // Rate 1/2, K=7 - Sync drift, moderate burst errors
        BCH_CODEC,           // BCH(15,7,2) - Random bit errors, cosmic radiation
        LDPC_CODEC,          // Rate 2/3 - Gaussian noise, near-Shannon performance
        TURBO_CODEC,         // Parallel concatenated - Severe noise conditions
        INTERLEAVER_ONLY,    // Burst error protection through data spreading
        NO_CORRECTION_NEEDED // Minimal errors, no correction required
    }
    
    /**
     * Correction strategy with algorithm and configuration
     */
    public static class CorrectionStrategy {
        public final CorrectionAlgorithm primaryAlgorithm;
        public final CorrectionAlgorithm secondaryAlgorithm;
        public final String configuration;
        public final int iterations;
        public final double expectedCodingGain;
        
        public CorrectionStrategy(CorrectionAlgorithm primary, CorrectionAlgorithm secondary, 
                                String config, int iterations, double codingGain) {
            this.primaryAlgorithm = primary;
            this.secondaryAlgorithm = secondary;
            this.configuration = config;
            this.iterations = iterations;
            this.expectedCodingGain = codingGain;
        }
        
        @Override
        public String toString() {
            return String.format(
                "STRATEGY: %s%s | Config: %s | Iterations: %d | Gain: %.1f dB",
                primaryAlgorithm,
                secondaryAlgorithm != null ? " + " + secondaryAlgorithm : "",
                configuration, iterations, expectedCodingGain
            );
        }
    }
    
    /**
     * Classifies error pattern and returns optimal correction strategy
     * Based on NASA satellite communication protocols and coding theory
     */
    public CorrectionStrategy classifyOptimalStrategy(ErrorAnalysis analysis) {
        // Decision tree based on satellite communication best practices
        if (analysis.errorDensity < 0.005) {
            return new CorrectionStrategy(
                CorrectionAlgorithm.NO_CORRECTION_NEEDED,
                null, "No correction", 0, 0.0
            );
        }
        
        // Priority-based classification for satellite environments
        switch (analysis.primaryErrorType) {
            case SYNC_DRIFT:
                return handleSyncDrift(analysis);
                
            case BURST_ERROR:
                return handleBurstErrors(analysis);
                
            case RANDOM_BIT_ERROR:
                return handleRandomBitErrors(analysis);
                
            case GAUSSIAN_NOISE:
                return handleGaussianNoise(analysis);
                
            case PACKET_LOSS:
                return handlePacketLoss(analysis);
                
            case MIXED_ERRORS:
                return handleMixedErrors(analysis);
                
            default:
                return handleDefaultCase(analysis);
        }
    }
    
    /**
     * Sync drift correction - Viterbi decoder with interleaving
     */
    private CorrectionStrategy handleSyncDrift(ErrorAnalysis analysis) {
        // Viterbi excels at sync recovery and moderate burst errors
        String config = String.format("Viterbi(R1/2,K7) + Interleaver(%d)", 
                                    calculateInterleaverDepth(analysis.burstErrorCount));
        
        return new CorrectionStrategy(
            CorrectionAlgorithm.VITERBI_DECODER,
            CorrectionAlgorithm.INTERLEAVER_ONLY,
            config, 8, // 8 iterations for convergence
            4.5 // 4.5 dB typical coding gain
        );
    }
    
    /**
     * Burst error correction - Interleaving + Viterdi/BCH combination
     */
    private CorrectionStrategy handleBurstErrors(ErrorAnalysis analysis) {
        if (analysis.burstErrorCount > 15) {
            // Severe bursts: Interleaving + Viterbi
            String config = String.format("DeepInterleaver(%d) + Viterbi(R1/2)", 
                                        calculateInterleaverDepth(analysis.burstErrorCount));
            
            return new CorrectionStrategy(
                CorrectionAlgorithm.INTERLEAVER_ONLY,
                CorrectionAlgorithm.VITERBI_DECODER,
                config, 6, 4.2
            );
        } else {
            // Moderate bursts: BCH for efficiency
            return new CorrectionStrategy(
                CorrectionAlgorithm.BCH_CODEC,
                CorrectionAlgorithm.INTERLEAVER_ONLY,
                "BCH(15,7,2) + LightInterleaving", 1, 3.8
            );
        }
    }
    
    /**
     * Random bit error correction - BCH codes for guaranteed correction
     */
    private CorrectionStrategy handleRandomBitErrors(ErrorAnalysis analysis) {
        // BCH provides guaranteed t-error correction (perfect for random errors)
        int errorCapability = calculateBCHCapability(analysis.totalBitErrors);
        String config = String.format("BCH(n=%d,k=7,t=%d)", 
                                    getBCHBlockSize(errorCapability), errorCapability);
        
        return new CorrectionStrategy(
            CorrectionAlgorithm.BCH_CODEC,
            null, config, 1, 3.5
        );
    }
    
    /**
     * Gaussian noise correction - LDPC for near-Shannon performance
     */
    private CorrectionStrategy handleGaussianNoise(ErrorAnalysis analysis) {
        // LDPC excels in AWGN channels with iterative decoding
        String config = String.format("LDPC(R2/3,IterativeBP) SNR=%.1fdB", 
                                    estimateChannelSNR(analysis.errorDensity));
        
        return new CorrectionStrategy(
            CorrectionAlgorithm.LDPC_CODEC,
            null, config, 20, // 20 iterations for belief propagation
            5.2 // Near-Shannon limit performance
        );
    }
    
    /**
     * Packet loss correction - Turbo codes for severe conditions
     */
    private CorrectionStrategy handlePacketLoss(ErrorAnalysis analysis) {
        // Turbo codes handle severe corruption and packet loss
        return new CorrectionStrategy(
            CorrectionAlgorithm.TURBO_CODEC,
            CorrectionAlgorithm.INTERLEAVER_ONLY,
            "Turbo(R1/3,BCJR) + Interleaver", 8, 4.8
        );
    }
    
    /**
     * Mixed error correction - Cascaded approach
     */
    private CorrectionStrategy handleMixedErrors(ErrorAnalysis analysis) {
        // For mixed errors, use Viterbi + BCH cascade
        if (analysis.burstErrorScore > 0.1 && analysis.randomErrorScore > 0.05) {
            return new CorrectionStrategy(
                CorrectionAlgorithm.VITERBI_DECODER,
                CorrectionAlgorithm.BCH_CODEC,
                "Viterbi→BCH Cascade", 10, 4.0
            );
        } else {
            // Default to LDPC for general mixed errors
            return handleGaussianNoise(analysis);
        }
    }
    
    /**
     * Default conservative strategy
     */
    private CorrectionStrategy handleDefaultCase(ErrorAnalysis analysis) {
        return new CorrectionStrategy(
            CorrectionAlgorithm.VITERBI_DECODER,
            null, "Viterbi(R1/2) Conservative", 6, 4.0
        );
    }
    
    // Scientific calculation methods
    
    private int calculateInterleaverDepth(int burstCount) {
        // Deeper interleaving for more severe bursts
        if (burstCount > 20) return 16;
        if (burstCount > 10) return 12;
        if (burstCount > 5) return 8;
        return 4;
    }
    
    private int calculateBCHCapability(int totalErrors) {
        // Select BCH error correction capability based on error density
        if (totalErrors < 50) return 2;  // BCH(15,7,2)
        if (totalErrors < 100) return 3; // BCH(31,16,3)
        return 4; // BCH(63,45,4) for heavier errors
    }
    
    private int getBCHBlockSize(int errorCapability) {
        switch (errorCapability) {
            case 2: return 15;
            case 3: return 31;
            case 4: return 63;
            default: return 15;
        }
    }
    
    private double estimateChannelSNR(double errorDensity) {
        // Convert error density to estimated SNR (simplified model)
        return 12.0 - (errorDensity * 50.0); // dB scale approximation
    }
    
}