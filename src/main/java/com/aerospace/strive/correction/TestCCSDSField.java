package com.aerospace.strive.correction;

/**
 * Test the CCSDS Galois Field implementation
 */
public class TestCCSDSField {
    public static void main(String[] args) {
        System.out.println("🧪 Testing CCSDS Galois Field GF(256)");
        System.out.println("======================================");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test basic arithmetic
        byte a = 0x53;
        byte b = (byte) 0xCA;  // Fixed: added cast to byte
        
        byte mult = gf.multiply(a, b);
        byte div = gf.divide(mult, b);
        
        System.out.println("Basic Arithmetic:");
        System.out.printf("0x%02X * 0x%02X = 0x%02X%n", a, b, mult);
        System.out.printf("0x%02X / 0x%02X = 0x%02X %s%n", mult, b, div, 
                         div == a ? "✅" : "❌");
        
        // Test inverses
        byte inverse = gf.inverse(a);
        byte shouldBeOne = gf.multiply(a, inverse);
        System.out.printf("Inverse of 0x%02X = 0x%02X, product = 0x%02X %s%n", 
                         a, inverse, shouldBeOne, shouldBeOne == 1 ? "✅" : "❌");
        
        // Test powers
        byte alpha = 0x02;
        byte alphaSquared = gf.multiply(alpha, alpha);
        byte alphaPower2 = gf.power(alpha, 2);
        System.out.printf("α²: multiply = 0x%02X, power = 0x%02X %s%n",
                         alphaSquared, alphaPower2, 
                         alphaSquared == alphaPower2 ? "✅" : "❌");
        
        System.out.println("✅ CCSDS Field test completed!");
    }
}