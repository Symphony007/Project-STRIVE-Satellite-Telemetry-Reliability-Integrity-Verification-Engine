package correction;

/**
 * NASA-GRADE ERROR PATTERN ANALYSIS ENGINE
 * Standalone analysis without external dependencies
 */
public class ErrorPatternAnalyzer {
    
    // Enhanced thresholds based on algorithm performance testing
    private static final double BURST_ERROR_THRESHOLD = 0.20;
    private static final double RANDOM_ERROR_THRESHOLD = 0.15;
    private static final double SYNC_DRIFT_THRESHOLD = 0.25;
    private static final double PACKET_LOSS_THRESHOLD = 0.30;
    private static final double GAUSSIAN_NOISE_THRESHOLD = 0.15;
    
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
        public final boolean requiresCorrection;
        
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
            this.requiresCorrection = errorDensity > 0.01;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ERROR_ANALYSIS [%s] BitErrors: %d, Density: %.1f%%, Correct: %s",
                primaryErrorType, totalBitErrors, errorDensity * 100, requiresCorrection
            );
        }
    }
    
    /**
     * Satellite error types for classification
     */
    public enum ErrorType {
        BURST_ERROR,
        RANDOM_BIT_ERROR, 
        SYNC_DRIFT,
        PACKET_LOSS,
        GAUSSIAN_NOISE,
        MIXED_ERRORS,
        MINOR_CORRUPTION
    }
    
    /**
     * Enhanced error pattern analysis
     */
    public ErrorAnalysis analyzeErrorPattern(byte[] originalFrame, byte[] corruptedFrame) {
        int totalBitErrors = countBitErrors(originalFrame, corruptedFrame);
        double errorDensity = (double) totalBitErrors / (originalFrame.length * 8);
        
        int burstErrorCount = analyzeBurstErrors(originalFrame, corruptedFrame);
        double burstErrorScore = totalBitErrors > 0 ? (double) burstErrorCount / totalBitErrors : 0;
        
        double randomErrorScore = analyzeRandomErrorPattern(originalFrame, corruptedFrame, totalBitErrors);
        double syncDriftScore = analyzeSyncDriftPattern(originalFrame, corruptedFrame);
        double packetLossScore = analyzePacketLossPattern(originalFrame, corruptedFrame);
        double gaussianNoiseScore = analyzeGaussianNoisePattern(originalFrame, corruptedFrame, errorDensity);
        
        ErrorType primaryType = classifyPrimaryError(
            burstErrorScore, randomErrorScore, syncDriftScore, 
            packetLossScore, gaussianNoiseScore, errorDensity, totalBitErrors
        );
        
        return new ErrorAnalysis(
            primaryType, burstErrorScore, randomErrorScore, syncDriftScore,
            packetLossScore, gaussianNoiseScore, totalBitErrors, burstErrorCount, errorDensity
        );
    }
    
    /**
     * Burst error detection
     */
    private int analyzeBurstErrors(byte[] original, byte[] corrupted) {
        int burstCount = 0;
        boolean inBurst = false;
        int consecutiveErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            int byteErrors = Integer.bitCount(original[i] ^ corrupted[i]);
            
            if (byteErrors >= 2) {
                consecutiveErrors++;
                if (!inBurst && consecutiveErrors >= 3) {
                    inBurst = true;
                    burstCount++;
                }
            } else {
                if (inBurst && consecutiveErrors > 0) {
                    inBurst = false;
                }
                consecutiveErrors = 0;
            }
        }
        
        return burstCount;
    }
    
    /**
     * Random error analysis
     */
    private double analyzeRandomErrorPattern(byte[] original, byte[] corrupted, int totalErrors) {
        if (totalErrors == 0) return 0.0;
        
        int[] byteErrors = new int[original.length];
        int totalByteErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            byteErrors[i] = Integer.bitCount(original[i] ^ corrupted[i]);
            totalByteErrors += byteErrors[i];
        }
        
        double mean = (double) totalByteErrors / original.length;
        if (mean == 0) return 0.0;
        
        double variance = 0;
        for (int errors : byteErrors) {
            variance += Math.pow(errors - mean, 2);
        }
        variance /= original.length;
        
        double randomness = 1.0 / (1.0 + Math.sqrt(variance));
        return Math.min(randomness, 1.0);
    }
    
    /**
     * Sync drift detection
     */
    private double analyzeSyncDriftPattern(byte[] original, byte[] corrupted) {
        boolean[] origBits = bytesToBits(original);
        boolean[] corruptBits = bytesToBits(corrupted);
        
        double bestMatch = 0.0;
        
        for (int shift = -16; shift <= 16; shift++) {
            if (shift == 0) continue;
            double matchScore = calculateShiftMatch(origBits, corruptBits, shift);
            if (matchScore > bestMatch) bestMatch = matchScore;
        }
        
        return bestMatch;
    }
    
    /**
     * Packet loss detection
     */
    private double analyzePacketLossPattern(byte[] original, byte[] corrupted) {
        int zeroSequences = 0;
        int currentZeroLength = 0;
        
        for (int i = 0; i < corrupted.length; i++) {
            if (corrupted[i] == 0 && original[i] != 0) {
                currentZeroLength++;
            } else {
                if (currentZeroLength >= 4) zeroSequences++;
                currentZeroLength = 0;
            }
        }
        
        if (currentZeroLength >= 4) zeroSequences++;
        return (double) zeroSequences / Math.max(1, corrupted.length / 8);
    }
    
    /**
     * Gaussian noise analysis
     */
    private double analyzeGaussianNoisePattern(byte[] original, byte[] corrupted, double errorDensity) {
        if (errorDensity == 0) return 0.0;
        
        int[] bitErrorsPerByte = new int[original.length];
        int totalErrors = 0;
        
        for (int i = 0; i < original.length; i++) {
            bitErrorsPerByte[i] = Integer.bitCount(original[i] ^ corrupted[i]);
            totalErrors += bitErrorsPerByte[i];
        }
        
        double mean = (double) totalErrors / original.length;
        if (mean == 0) return 0.0;
        
        double stdDev = 0;
        for (int errors : bitErrorsPerByte) {
            stdDev += Math.pow(errors - mean, 2);
        }
        stdDev = Math.sqrt(stdDev / original.length);
        
        double cv = stdDev / mean;
        return Math.max(0, 1.0 - cv);
    }
    
    /**
     * Enhanced error classification
     */
    private ErrorType classifyPrimaryError(double burstScore, double randomScore, 
                                         double syncScore, double lossScore, 
                                         double noiseScore, double errorDensity, int totalErrors) {
        
        if (errorDensity < 0.005 || totalErrors < 10) {
            return ErrorType.MINOR_CORRUPTION;
        }
        
        if (lossScore > PACKET_LOSS_THRESHOLD) return ErrorType.PACKET_LOSS;
        if (syncScore > SYNC_DRIFT_THRESHOLD && burstScore < 0.4) return ErrorType.SYNC_DRIFT;
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
            if (original[i] == corrupted[i - shift]) matches++;
            comparisons++;
        }
        
        return comparisons > 0 ? (double) matches / comparisons : 0.0;
    }
}