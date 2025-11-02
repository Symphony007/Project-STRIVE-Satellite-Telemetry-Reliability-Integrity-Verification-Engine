package correction;

import frames.TelemetryFrame;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * NASA-GRADE ERROR PATTERN ANALYSIS ENGINE
 * Diagnoses satellite communication error types through scientific pattern recognition
 * Identifies burst errors, random bit errors, sync drift, packet loss, and Gaussian noise
 */
public class ErrorPatternAnalyzer {
    
    // Error pattern thresholds based on satellite communication research
    private static final double BURST_ERROR_THRESHOLD = 0.15;    // Consecutive error density
    private static final double RANDOM_ERROR_THRESHOLD = 0.02;   // Sparse error distribution  
    private static final double SYNC_DRIFT_THRESHOLD = 0.10;     // Bit shift patterns
    private static final double PACKET_LOSS_THRESHOLD = 0.30;    // Zero-byte sequences
    private static final double GAUSSIAN_NOISE_THRESHOLD = 0.08; // Uniform error distribution
    
    /**
     * Comprehensive error analysis results
     */
    public static class ErrorAnalysis {
        public final ErrorType primaryErrorType;
        public final double burstErrorScore;
        public final double randomErrorScore;
        public final double syncDriftScore;
        public final double packetLossScore;
        public final double gaussianNoiseScore;
        public final int totalBitErrors;
        public final int burstErrorCount;
        public final double errorDensity;
        
        public ErrorAnalysis(ErrorType primaryErrorType, double burstErrorScore, 
                           double randomErrorScore, double syncDriftScore, 
                           double packetLossScore, double gaussianNoiseScore,
                           int totalBitErrors, int burstErrorCount, double errorDensity) {
            this.primaryErrorType = primaryErrorType;
            this.burstErrorScore = burstErrorScore;
            this.randomErrorScore = randomErrorScore;
            this.syncDriftScore = syncDriftScore;
            this.packetLossScore = packetLossScore;
            this.gaussianNoiseScore = gaussianNoiseScore;
            this.totalBitErrors = totalBitErrors;
            this.burstErrorCount = burstErrorCount;
            this.errorDensity = errorDensity;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ERROR_ANALYSIS [%s] BitErrors: %d, Bursts: %d, Density: %.3f",
                primaryErrorType, totalBitErrors, burstErrorCount, errorDensity
            );
        }
    }
    
    /**
     * Satellite error types for classification
     */
    public enum ErrorType {
        BURST_ERROR,           // Solar flares/radiation
        RANDOM_BIT_ERROR,      // Cosmic radiation  
        SYNC_DRIFT,            // Doppler/oscillator issues
        PACKET_LOSS,           // Atmospheric scintillation
        GAUSSIAN_NOISE,        // Thermal/RF interference
        MIXED_ERRORS,          // Multiple error types
        MINOR_CORRUPTION       // Low-level errors
    }
    
    /**
     * Analyzes corrupted frame to diagnose error patterns
     * @param originalFrame Clean reference frame
     * @param corruptedFrame Frame with errors
     * @return Scientific analysis of error characteristics
     */
    public ErrorAnalysis analyzeErrorPattern(byte[] originalFrame, byte[] corruptedFrame) {
        // Basic error statistics
        int totalBitErrors = countBitErrors(originalFrame, corruptedFrame);
        double errorDensity = (double) totalBitErrors / (originalFrame.length * 8);
        
        // Advanced pattern analysis
        int burstErrorCount = analyzeBurstErrors(originalFrame, corruptedFrame);
        double burstErrorScore = (double) burstErrorCount / totalBitErrors;
        
        double randomErrorScore = analyzeRandomErrorPattern(originalFrame, corruptedFrame, totalBitErrors);
        double syncDriftScore = analyzeSyncDriftPattern(originalFrame, corruptedFrame);
        double packetLossScore = analyzePacketLossPattern(originalFrame, corruptedFrame);
        double gaussianNoiseScore = analyzeGaussianNoisePattern(originalFrame, corruptedFrame, errorDensity);
        
        // Determine primary error type
        ErrorType primaryType = classifyPrimaryError(
            burstErrorScore, randomErrorScore, syncDriftScore, 
            packetLossScore, gaussianNoiseScore, errorDensity
        );
        
        return new ErrorAnalysis(
            primaryType, burstErrorScore, randomErrorScore, syncDriftScore,
            packetLossScore, gaussianNoiseScore, totalBitErrors, burstErrorCount, errorDensity
        );
    }
    
    /**
     * Detects burst error patterns (consecutive bit errors)
     */
    private int analyzeBurstErrors(byte[] original, byte[] corrupted) {
        int burstCount = 0;
        boolean inBurst = false;
        int consecutiveErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            byte xor = (byte) (original[i] ^ corrupted[i]);
            
            if (xor != 0) {
                // Check individual bits in the byte
                for (int bit = 0; bit < 8; bit++) {
                    if ((xor & (1 << bit)) != 0) {
                        consecutiveErrors++;
                        if (!inBurst && consecutiveErrors >= 3) {
                            inBurst = true;
                            burstCount++;
                        }
                    } else {
                        if (inBurst && consecutiveErrors > 0) {
                            inBurst = false;
                            consecutiveErrors = 0;
                        }
                    }
                }
            } else {
                if (inBurst && consecutiveErrors > 0) {
                    inBurst = false;
                    consecutiveErrors = 0;
                }
            }
        }
        
        return burstCount;
    }
    
    /**
     * Analyzes random error distribution pattern
     */
    private double analyzeRandomErrorPattern(byte[] original, byte[] corrupted, int totalErrors) {
        if (totalErrors == 0) return 0.0;
        
        // Calculate error distribution uniformity
        int[] byteErrors = new int[original.length];
        int totalByteErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            byteErrors[i] = Integer.bitCount(original[i] ^ corrupted[i]);
            totalByteErrors += byteErrors[i];
        }
        
        // For random errors, distribution should be relatively uniform
        double mean = (double) totalByteErrors / original.length;
        double variance = 0;
        
        for (int errors : byteErrors) {
            variance += Math.pow(errors - mean, 2);
        }
        variance /= original.length;
        
        // Lower variance indicates more random distribution
        double randomness = 1.0 / (1.0 + variance);
        return Math.min(randomness, 1.0);
    }
    
    /**
     * Detects sync drift patterns (bit shifting)
     */
    private double analyzeSyncDriftPattern(byte[] original, byte[] corrupted) {
        // Convert to bit arrays for shift analysis
        boolean[] origBits = bytesToBits(original);
        boolean[] corruptBits = bytesToBits(corrupted);
        
        double bestMatch = 0.0;
        
        // Test various bit shifts (-8 to +8 bits)
        for (int shift = -8; shift <= 8; shift++) {
            if (shift == 0) continue;
            
            double matchScore = calculateShiftMatch(origBits, corruptBits, shift);
            bestMatch = Math.max(bestMatch, matchScore);
        }
        
        return bestMatch;
    }
    
    /**
     * Detects packet loss patterns (zero sequences)
     */
    private double analyzePacketLossPattern(byte[] original, byte[] corrupted) {
        int zeroSequences = 0;
        int currentZeroLength = 0;
        
        for (int i = 0; i < corrupted.length; i++) {
            if (corrupted[i] == 0 && original[i] != 0) {
                currentZeroLength++;
            } else {
                if (currentZeroLength >= 3) { // Sequence of 3+ zero bytes indicates packet loss
                    zeroSequences++;
                }
                currentZeroLength = 0;
            }
        }
        
        // Check final sequence
        if (currentZeroLength >= 3) {
            zeroSequences++;
        }
        
        return (double) zeroSequences / (corrupted.length / 10.0); // Normalize
    }
    
    /**
     * Analyzes Gaussian noise patterns (uniform error distribution)
     */
    private double analyzeGaussianNoisePattern(byte[] original, byte[] corrupted, double errorDensity) {
        if (errorDensity == 0) return 0.0;
        
        // Gaussian noise typically shows uniform error distribution
        int[] bitErrorsPerByte = new int[original.length];
        int totalErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            bitErrorsPerByte[i] = Integer.bitCount(original[i] ^ corrupted[i]);
            totalErrors += bitErrorsPerByte[i];
        }
        
        // Calculate coefficient of variation
        double mean = (double) totalErrors / original.length;
        if (mean == 0) return 0.0;
        
        double stdDev = 0;
        for (int errors : bitErrorsPerByte) {
            stdDev += Math.pow(errors - mean, 2);
        }
        stdDev = Math.sqrt(stdDev / original.length);
        
        double cv = stdDev / mean; // Coefficient of variation
        
        // Low CV indicates Gaussian distribution
        return Math.max(0, 1.0 - cv);
    }
    
    /**
     * Classifies primary error type based on pattern scores
     */
    private ErrorType classifyPrimaryError(double burstScore, double randomScore, 
                                         double syncScore, double lossScore, 
                                         double noiseScore, double errorDensity) {
        
        if (errorDensity < 0.001) return ErrorType.MINOR_CORRUPTION;
        
        // Priority classification based on satellite communication experience
        if (lossScore > PACKET_LOSS_THRESHOLD) return ErrorType.PACKET_LOSS;
        if (syncScore > SYNC_DRIFT_THRESHOLD) return ErrorType.SYNC_DRIFT;
        if (burstScore > BURST_ERROR_THRESHOLD) return ErrorType.BURST_ERROR;
        if (noiseScore > GAUSSIAN_NOISE_THRESHOLD) return ErrorType.GAUSSIAN_NOISE;
        if (randomScore > RANDOM_ERROR_THRESHOLD) return ErrorType.RANDOM_BIT_ERROR;
        
        return ErrorType.MIXED_ERRORS;
    }
    
    // Utility methods
    private int countBitErrors(byte[] original, byte[] corrupted) {
        int errors = 0;
        for (int i = 0; i < original.length; i++) {
            errors += Integer.bitCount(original[i] ^ corrupted[i]);
        }
        return errors;
    }
    
    private boolean[] bytesToBits(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = ((bytes[i] >> (7 - j)) & 1) == 1;
            }
        }
        return bits;
    }
    
    private double calculateShiftMatch(boolean[] original, boolean[] corrupted, int shift) {
        int matches = 0;
        int comparisons = 0;
        
        int start = Math.max(0, shift);
        int end = Math.min(original.length, corrupted.length + shift);
        
        for (int i = start; i < end; i++) {
            if (original[i] == corrupted[i - shift]) {
                matches++;
            }
            comparisons++;
        }
        
        return comparisons > 0 ? (double) matches / comparisons : 0.0;
    }
    
}