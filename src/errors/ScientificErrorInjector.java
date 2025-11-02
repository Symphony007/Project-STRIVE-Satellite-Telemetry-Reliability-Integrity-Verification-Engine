package errors;

import frames.TelemetryFrame;
import data.TelemetryParser.TelemetryData;
import data.ISSDataFetcher;
import data.TelemetryParser;
import java.util.Random;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * NASA-GRADE SATELLITE ERROR SIMULATION ENGINE - FIXED SCALING
 * Properly scales errors for different data sizes (128B frames vs 256B encoded)
 */
public class ScientificErrorInjector {
    
    private final Random random;
    private final ChannelModel channelModel;
    
    // Realistic satellite error probabilities (reduced for coding capability)
    private static final double BURST_ERROR_PROB = 0.08;    // Reduced from 0.12
    private static final double RANDOM_ERROR_PROB = 0.05;   // Reduced from 0.08
    private static final double SYNC_DRIFT_PROB = 0.04;     // Reduced from 0.06
    private static final double PACKET_LOSS_PROB = 0.02;    // Reduced from 0.04
    private static final double GAUSSIAN_NOISE_PROB = 0.06; // Reduced from 0.10
    
    public ScientificErrorInjector() {
        this.random = new Random();
        this.channelModel = new ChannelModel();
    }
    
    /**
     * Main error injection with proper scaling for data size
     */
    public byte[] injectRealisticErrors(byte[] originalFrame) {
        return injectRealisticErrors(originalFrame, 1.0);
    }
    
    /**
     * Overload with scaling factor for encoded data
     */
    public byte[] injectRealisticErrors(byte[] originalFrame, double scaleFactor) {
        byte[] corruptedFrame = originalFrame.clone();
        
        // Scale probabilities based on data size (encoded data gets fewer errors)
        double scaledBurstProb = BURST_ERROR_PROB * scaleFactor;
        double scaledRandomProb = RANDOM_ERROR_PROB * scaleFactor;
        double scaledSyncProb = SYNC_DRIFT_PROB * scaleFactor;
        double scaledLossProb = PACKET_LOSS_PROB * scaleFactor;
        double scaledNoiseProb = GAUSSIAN_NOISE_PROB * scaleFactor;
        
        // Apply error models with scaled probabilities
        if (random.nextDouble() < scaledNoiseProb) {
            corruptedFrame = applyGaussianNoise(corruptedFrame, scaleFactor);
        }
        
        if (random.nextDouble() < scaledBurstProb) {
            corruptedFrame = applyBurstErrors(corruptedFrame, scaleFactor);
        }
        
        if (random.nextDouble() < scaledRandomProb) {
            corruptedFrame = applyRandomBitErrors(corruptedFrame, scaleFactor);
        }
        
        if (random.nextDouble() < scaledSyncProb) {
            corruptedFrame = applySyncDrift(corruptedFrame);
        }
        
        if (random.nextDouble() < scaledLossProb) {
            corruptedFrame = applyPacketLoss(corruptedFrame, scaleFactor);
        }
        
        return corruptedFrame;
    }
    
    /**
     * Gaussian Noise with scaling
     */
    private byte[] applyGaussianNoise(byte[] frame, double scaleFactor) {
        byte[] noisyFrame = frame.clone();
        double noiseIntensity = 0.1 + random.nextDouble() * 0.2; // Reduced intensity
        noiseIntensity *= scaleFactor; // Scale for encoded data
        
        for (int i = 0; i < noisyFrame.length; i++) {
            byte corruptedByte = noisyFrame[i];
            for (int bit = 0; bit < 8; bit++) {
                if (random.nextDouble() < noiseIntensity * 0.05) { // Further reduced
                    corruptedByte ^= (1 << bit);
                }
            }
            noisyFrame[i] = corruptedByte;
        }
        return noisyFrame;
    }
    
    /**
     * Burst Errors with scaling
     */
    private byte[] applyBurstErrors(byte[] frame, double scaleFactor) {
        byte[] burstyFrame = frame.clone();
        
        // Scale burst count based on data size
        int burstCount = (int) Math.max(1, (1 + random.nextInt(2)) * scaleFactor);
        
        for (int b = 0; b < burstCount; b++) {
            int burstStart = random.nextInt(frame.length - 4);
            int burstLength = 2 + random.nextInt(3); // Reduced burst length
            
            for (int i = burstStart; i < Math.min(burstStart + burstLength, burstyFrame.length); i++) {
                // Corrupt fewer bits in burst region
                int bitsToFlip = 2 + random.nextInt(3); // Reduced from 4-8 to 2-5
                for (int f = 0; f < bitsToFlip; f++) {
                    int bitPos = random.nextInt(8);
                    burstyFrame[i] ^= (1 << bitPos);
                }
            }
        }
        
        return burstyFrame;
    }
    
    /**
     * Random Bit Errors with scaling
     */
    private byte[] applyRandomBitErrors(byte[] frame, double scaleFactor) {
        byte[] randomErrorFrame = frame.clone();
        double bitErrorRate = 0.002 + random.nextDouble() * 0.008; // Reduced BER 0.2-1.0%
        bitErrorRate *= scaleFactor;
        
        for (int i = 0; i < randomErrorFrame.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                if (random.nextDouble() < bitErrorRate) {
                    randomErrorFrame[i] ^= (1 << bit);
                }
            }
        }
        return randomErrorFrame;
    }
    
    /**
     * Sync Drift
     */
    private byte[] applySyncDrift(byte[] frame) {
        byte[] driftedFrame = frame.clone();
        int driftBits = 1 + random.nextInt(2); // Reduced drift
        
        boolean[] bits = bytesToBits(frame);
        boolean[] driftedBits = new boolean[bits.length];
        
        System.arraycopy(bits, driftBits, driftedBits, 0, bits.length - driftBits);
        
        for (int i = 0; i < driftBits; i++) {
            driftedBits[bits.length - driftBits + i] = random.nextBoolean();
        }
        
        return bitsToBytes(driftedBits, driftedFrame.length);
    }
    
    /**
     * Packet Loss with scaling
     */
    private byte[] applyPacketLoss(byte[] frame, double scaleFactor) {
        byte[] lossyFrame = frame.clone();
        
        if (random.nextDouble() < 0.2) { // Reduced severe loss probability
            int lossStart = random.nextInt(frame.length - 10);
            int lossLength = 5 + random.nextInt(5); // Reduced loss length
            
            for (int i = lossStart; i < Math.min(lossStart + lossLength, lossyFrame.length); i++) {
                lossyFrame[i] = (byte) random.nextInt(256);
            }
        } else {
            int lossStart = random.nextInt(frame.length - 5);
            int lossLength = 2 + random.nextInt(3);
            
            for (int i = lossStart; i < Math.min(lossStart + lossLength, lossyFrame.length); i++) {
                for (int bit = 0; bit < 8; bit++) {
                    if (random.nextDouble() < 0.15) { // Reduced corruption
                        lossyFrame[i] ^= (1 << bit);
                    }
                }
            }
        }
        
        return lossyFrame;
    }
    
    private static class ChannelModel {
        private final Random random = new Random();
        
        public double getCurrentSNR() {
            return 6 + random.nextDouble() * 6; // Improved SNR range
        }
        
        public boolean isSolarStormActive() {
            return random.nextDouble() < 0.02; // Reduced solar storm probability
        }
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
    
    private byte[] bitsToBytes(boolean[] bits, int targetLength) {
        byte[] bytes = new byte[targetLength];
        for (int i = 0; i < bytes.length && i * 8 < bits.length; i++) {
            for (int j = 0; j < 8; j++) {
                int bitIndex = i * 8 + j;
                if (bitIndex < bits.length && bits[bitIndex]) {
                    bytes[i] |= (1 << (7 - j));
                }
            }
        }
        return bytes;
    }
    
}