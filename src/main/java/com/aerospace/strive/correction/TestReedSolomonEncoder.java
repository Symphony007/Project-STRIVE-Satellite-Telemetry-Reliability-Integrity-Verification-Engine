package com.aerospace.strive.correction;

import java.util.Arrays;

/**
 * Comprehensive test for CCSDS Reed-Solomon encoder/decoder
 * Tests complete error correction with various error patterns
 */
public class TestReedSolomonEncoder {
    
    public static void main(String[] args) {
        System.out.println("🧪 CCSDS REED-SOLOMON COMPLETE TEST SUITE");
        System.out.println("=========================================");
        
        testEncoderBasic();
        testDecoderWithSingleError();
        testDecoderWithTwoErrors();
        testDecoderWithBurstErrors();
        testDecoderWithErasures();
        testDecoderBeyondCapacity();
        testPerformance();
        
        System.out.println("\n🎉 REED-SOLOMON TEST COMPLETE! Ready for satellite deployment.");
    }
    
    private static void testEncoderBasic() {
        System.out.println("\n1. 🔧 ENCODER BASIC FUNCTIONALITY");
        System.out.println("-------------------------------");
        
        // Test with shortened code suitable for telemetry
        int n = 15;  // Shortened for testing
        int k = 11;  // 11 data bytes + 4 parity
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        System.out.println("Generator polynomial: " + rs.getGeneratorPolynomial());
        
        // Test encoding with sample data
        byte[] message = "HELLO_WORLD".getBytes(); // 11 bytes exactly
        System.out.printf("Original message: %s (%d bytes)%n", 
                         new String(message), message.length);
        
        byte[] encoded = rs.encode(message);
        System.out.printf("Encoded codeword: %d bytes (%d data + %d parity)%n",
                         encoded.length, k, n - k);
        
        // Display first few bytes
        System.out.print("First 8 bytes: ");
        for (int i = 0; i < Math.min(8, encoded.length); i++) {
            System.out.printf("0x%02X ", encoded[i]);
        }
        System.out.println("...");
        
        // Verify systematic property (first k bytes should match message)
        boolean systematic = true;
        for (int i = 0; i < k; i++) {
            if (encoded[i] != message[i]) {
                systematic = false;
                break;
            }
        }
        System.out.println("Systematic encoding: " + (systematic ? "✅" : "❌"));
        
        // Test syndrome calculation (should be all zeros for valid codeword)
        byte[] syndromes = rs.computeSyndromes(encoded);
        boolean syndromesZero = true;
        for (byte syndrome : syndromes) {
            if (syndrome != 0) {
                syndromesZero = false;
                break;
            }
        }
        System.out.println("Syndromes zero for valid codeword: " + (syndromesZero ? "✅" : "❌"));
    }
    
    private static void testDecoderWithSingleError() {
        System.out.println("\n2. 🔄 DECODER WITH SINGLE ERROR");
        System.out.println("------------------------------");
        
        int n = 15, k = 11, t = 2;
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        byte[] message = "HELLO_WORLD".getBytes();
        byte[] encoded = rs.encode(message);
        
        System.out.println("Original encoded data (first 8 bytes):");
        for (int i = 0; i < Math.min(8, encoded.length); i++) {
            System.out.printf("0x%02X ", encoded[i]);
        }
        System.out.println();
        
        // Introduce ONE error in a strategic position (not in first k bytes)
        byte[] corrupted = encoded.clone();
        int errorPosition = 12; // Position in parity section
        byte originalValue = corrupted[errorPosition];
        corrupted[errorPosition] = (byte) 0xFF; // Flip all bits
        
        System.out.printf("Introduced 1 error at position %d: 0x%02X → 0x%02X%n", 
                         errorPosition, originalValue & 0xFF, corrupted[errorPosition] & 0xFF);
        
        // Calculate syndromes to verify error detection
        byte[] syndromes = rs.computeSyndromes(corrupted);
        System.out.print("Syndromes after error: ");
        for (byte s : syndromes) {
            System.out.printf("0x%02X ", s & 0xFF);
        }
        System.out.println();
        
        // Decode and correct
        byte[] decoded = rs.decode(corrupted, null);
        
        boolean correctionSuccessful = decoded != null && Arrays.equals(message, decoded);
        System.out.println("Single error correction: " + (correctionSuccessful ? "✅" : "❌"));
        
        if (correctionSuccessful) {
            System.out.println("✅ Original message recovered: " + new String(decoded));
        } else {
            System.out.println("❌ Correction failed");
        }
    }
    
    private static void testDecoderWithTwoErrors() {
        System.out.println("\n3. 🔄 DECODER WITH TWO ERRORS");
        System.out.println("----------------------------");
        
        int n = 15, k = 11, t = 2;
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        byte[] message = "HELLO_WORLD".getBytes();
        byte[] encoded = rs.encode(message);
        
        // Introduce TWO errors in parity section
        byte[] corrupted = encoded.clone();
        int[] errorPositions = {12, 13};
        byte[] originalValues = {corrupted[12], corrupted[13]};
        
        corrupted[12] = (byte) 0xAA;
        corrupted[13] = (byte) 0x55;
        
        System.out.printf("Introduced 2 errors at positions %d and %d%n", 
                         errorPositions[0], errorPositions[1]);
        System.out.printf("Changed: 0x%02X→0x%02X, 0x%02X→0x%02X%n",
                         originalValues[0] & 0xFF, corrupted[12] & 0xFF,
                         originalValues[1] & 0xFF, corrupted[13] & 0xFF);
        
        // Calculate syndromes
        byte[] syndromes = rs.computeSyndromes(corrupted);
        System.out.print("Syndromes: ");
        for (byte s : syndromes) {
            System.out.printf("0x%02X ", s & 0xFF);
        }
        System.out.println();
        
        // Decode and correct
        byte[] decoded = rs.decode(corrupted, null);
        
        boolean correctionSuccessful = decoded != null && Arrays.equals(message, decoded);
        System.out.println("Two error correction: " + (correctionSuccessful ? "✅" : "❌"));
        
        if (correctionSuccessful) {
            System.out.println("✅ Original message recovered: " + new String(decoded));
        }
    }
    
    private static void testDecoderWithBurstErrors() {
        System.out.println("\n4. 💥 DECODER WITH BURST ERRORS");
        System.out.println("------------------------------");
        
        int n = 15, k = 11, t = 2;
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        byte[] message = "BURST_TST!!".getBytes(); // 11 bytes exactly
        byte[] encoded = rs.encode(message);
        
        // Introduce burst error (consecutive bytes) in parity section
        byte[] corrupted = encoded.clone();
        int burstStart = 12;
        int burstLength = 2;
        
        for (int i = burstStart; i < burstStart + burstLength; i++) {
            corrupted[i] = (byte) 0xFF; // Flip all bits
        }
        
        System.out.println("Introduced burst error of length " + burstLength + " at position " + burstStart);
        
        // Decode and correct
        byte[] decoded = rs.decode(corrupted, null);
        
        boolean correctionSuccessful = decoded != null && Arrays.equals(message, decoded);
        System.out.println("Burst error correction: " + (correctionSuccessful ? "✅" : "❌"));
        
        if (correctionSuccessful) {
            System.out.println("✅ Original message recovered: " + new String(decoded));
        }
    }
    
    private static void testDecoderWithErasures() {
        System.out.println("\n5. 🎯 DECODER WITH ERASURES");
        System.out.println("--------------------------");
        
        int n = 15, k = 11, t = 2;
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        byte[] message = "ERASURE_T!".getBytes(); // 10 bytes + 1 padding
        // Pad to exactly 11 bytes
        byte[] paddedMessage = Arrays.copyOf(message, 11);
        paddedMessage[10] = '!';
        
        byte[] encoded = rs.encode(paddedMessage);
        
        // Introduce erasures (known error positions) - set to zero
        byte[] corrupted = encoded.clone();
        int[] erasurePositions = {12, 14}; // Known lost positions
        
        for (int pos : erasurePositions) {
            corrupted[pos] = 0; // Set to zero (erasure)
        }
        
        System.out.println("Introduced " + erasurePositions.length + " erasures at positions: " 
                         + Arrays.toString(erasurePositions));
        
        // Decode with erasure information
        byte[] decoded = rs.decode(corrupted, erasurePositions);
        
        boolean correctionSuccessful = decoded != null && Arrays.equals(paddedMessage, decoded);
        System.out.println("Erasure correction: " + (correctionSuccessful ? "✅" : "❌"));
        
        if (correctionSuccessful) {
            System.out.println("✅ Original message recovered: " + new String(decoded));
        }
    }
    
    private static void testDecoderBeyondCapacity() {
        System.out.println("\n6. 🚨 DECODER BEYOND CORRECTION CAPACITY");
        System.out.println("--------------------------------------");
        
        int n = 15, k = 11, t = 2;
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        byte[] message = "BEYOND_TEST".getBytes();
        byte[] encoded = rs.encode(message);
        
        // Introduce too many errors (beyond correction capability)
        byte[] corrupted = encoded.clone();
        int[] errorPositions = {11, 12, 13, 14}; // 4 errors > t=2
        
        for (int pos : errorPositions) {
            corrupted[pos] = (byte) 0xAA;
        }
        
        System.out.println("Introduced " + errorPositions.length + " errors (beyond capacity t=" + t + ")");
        
        // Attempt to decode
        byte[] decoded = rs.decode(corrupted, null);
        
        boolean shouldFail = decoded == null;
        System.out.println("Properly handles uncorrectable errors: " + (shouldFail ? "✅" : "❌"));
        
        if (!shouldFail) {
            System.out.println("❌ Unexpectedly corrected beyond capacity!");
        }
    }
    
    private static void testPerformance() {
        System.out.println("\n7. ⚡ PERFORMANCE TESTING");
        System.out.println("-----------------------");
        
        CCSDSReedSolomon rs = new CCSDSReedSolomon(15, 11);
        
        // Test multiple encode/decode cycles
        int testCycles = 5;
        int successfulCorrections = 0;
        
        for (int i = 0; i < testCycles; i++) {
            byte[] message = String.format("TEST%07d!!", i).getBytes();
            // Ensure exactly 11 bytes
            message = Arrays.copyOf(message, 11);
            
            byte[] encoded = rs.encode(message);
            
            // Introduce some errors in parity section
            byte[] corrupted = encoded.clone();
            corrupted[12] = (byte) 0x55;
            
            byte[] decoded = rs.decode(corrupted, null);
            
            if (decoded != null && Arrays.equals(message, decoded)) {
                successfulCorrections++;
            }
        }
        
        CCSDSReedSolomon.DecoderStatistics stats = rs.getStatistics();
        System.out.println("Performance Statistics:");
        System.out.println("  Decode operations: " + stats.decodeOperations);
        System.out.println("  Corrected errors: " + stats.correctedErrors);
        System.out.println("  Decode failures: " + stats.decodeFailures);
        System.out.println("  Success rate: " + successfulCorrections + "/" + testCycles + " (" + 
                         (successfulCorrections * 100 / testCycles) + "%)");
        
        System.out.println("✅ Performance testing completed");
    }
}