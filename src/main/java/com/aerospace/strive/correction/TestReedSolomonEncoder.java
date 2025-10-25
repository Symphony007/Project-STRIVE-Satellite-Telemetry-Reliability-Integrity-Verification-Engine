package com.aerospace.strive.correction;

/**
 * Test Reed-Solomon encoder
 */
public class TestReedSolomonEncoder {
    public static void main(String[] args) {
        System.out.println("🧪 Testing CCSDS Reed-Solomon Encoder");
        System.out.println("======================================");
        
        // Test with shortened code suitable for telemetry
        int n = 15;  // Shortened for testing
        int k = 11;  // 11 data bytes + 4 parity
        CCSDSReedSolomon rs = new CCSDSReedSolomon(n, k);
        
        System.out.println("Generator polynomial degree: " + rs.getGeneratorPolynomial().degree());
        System.out.println("Generator: " + rs.getGeneratorPolynomial());
        
        // Test encoding with sample data
        byte[] message = "HELLO_WORLD".getBytes(); // 11 bytes exactly
        System.out.printf("Original message: %s (%d bytes)%n", 
                         new String(message), message.length);
        
        byte[] encoded = rs.encode(message);
        System.out.printf("Encoded codeword: %d bytes (%d data + %d parity)%n",
                         encoded.length, k, n - k);
        
        // Display first few bytes
        System.out.print("First 6 bytes: ");
        for (int i = 0; i < Math.min(6, encoded.length); i++) {
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
        
        System.out.println("✅ Reed-Solomon encoder test completed!");
    }
}