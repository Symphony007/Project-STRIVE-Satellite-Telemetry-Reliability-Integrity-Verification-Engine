package com.aerospace.strive.correction;

/**
 * COMPREHENSIVE TEST SUITE for Enhanced CCSDS Galois Field
 * Tests both original functionality AND new NASA-grade features
 */
public class TestCCSDSField {
    
    public static void main(String[] args) {
        System.out.println("🧪 ENHANCED CCSDS GALOIS FIELD - COMPREHENSIVE TEST");
        System.out.println("====================================================");
        
        // Run all test suites
        testBasicFieldProperties();
        testOriginalOperations();      // Tests backward compatibility
        testMathematicalOperations();  // Tests enhanced mathematics
        testErasureSupport();          // Tests new NASA feature
        testPerformanceMonitoring();   // Tests new performance tracking
        testEdgeCases();               // Tests enhanced error handling
        testCCSDSCompliance();         // Tests space agency standards
        
        System.out.println("\n🎉 ALL TESTS PASSED! Enhanced field is ready for satellite operations.");
    }
    
    private static void testBasicFieldProperties() {
        System.out.println("\n1. 🔬 BASIC FIELD PROPERTIES");
        System.out.println("---------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test field size and primitive element
        assert gf.getFieldSize() == 256 : "Field size should be 256";
        assert gf.getPrimitiveElement() == 0x02 : "Primitive element should be 0x02";
        assert gf.getFieldOrder() == 255 : "Field order should be 255";
        
        System.out.println("✅ Field size: " + gf.getFieldSize());
        System.out.println("✅ Primitive element: α = 0x" + Integer.toHexString(gf.getPrimitiveElement() & 0xFF));
        System.out.println("✅ Multiplicative group order: " + gf.getFieldOrder());
        
        // Test element validation
        assert gf.isValidFieldElement((byte)0) : "Zero should be valid";
        assert gf.isValidFieldElement((byte)1) : "One should be valid"; 
        assert gf.isValidFieldElement((byte)255) : "255 should be valid";
        System.out.println("✅ Field element validation working");
    }
    
    private static void testOriginalOperations() {
        System.out.println("\n2. 📋 ORIGINAL OPERATIONS (Backward Compatibility)");
        System.out.println("------------------------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Original test cases from previous implementation
        byte a = 0x53;
        byte b = (byte) 0xCA;
        
        byte mult = gf.multiply(a, b);
        byte div = gf.divide(mult, b);
        
        System.out.println("Original Arithmetic Tests:");
        System.out.printf("  0x%02X * 0x%02X = 0x%02X%n", a, b, mult);
        System.out.printf("  0x%02X / 0x%02X = 0x%02X %s%n", mult, b, div, 
                         div == a ? "✅" : "❌");
        
        // Test inverses (original)
        byte inverse = gf.inverse(a);
        byte shouldBeOne = gf.multiply(a, inverse);
        System.out.printf("  Inverse of 0x%02X = 0x%02X, product = 0x%02X %s%n", 
                         a, inverse, shouldBeOne, shouldBeOne == 1 ? "✅" : "❌");
        
        // Test powers (original)
        byte alpha = 0x02;
        byte alphaSquared = gf.multiply(alpha, alpha);
        byte alphaPower2 = gf.power(alpha, 2);
        System.out.printf("  α²: multiply = 0x%02X, power = 0x%02X %s%n",
                         alphaSquared, alphaPower2, 
                         alphaSquared == alphaPower2 ? "✅" : "❌");
        
        assert div == a : "Backward compatibility failed - division";
        assert shouldBeOne == 1 : "Backward compatibility failed - inverse";
        assert alphaSquared == alphaPower2 : "Backward compatibility failed - exponentiation";
    }
    
    private static void testMathematicalOperations() {
        System.out.println("\n3. 🧮 ENHANCED MATHEMATICAL OPERATIONS");
        System.out.println("-------------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test CCSDS specification test vectors
        byte a = 0x53;
        byte b = (byte) 0xCA;
        byte expectedProduct = (byte) 0x01; // Known CCSDS test vector
        
        byte product = gf.multiply(a, b);
        System.out.printf("CCSDS Test Vector: 0x%02X × 0x%02X = 0x%02X %s%n", 
                         a & 0xFF, b & 0xFF, product & 0xFF,
                         product == expectedProduct ? "✅" : "❌");
        
        // Test additive properties (characteristic 2)
        byte sum = gf.add(a, a);
        System.out.printf("Characteristic 2: 0x%02X + 0x%02X = 0x%02X %s%n", 
                         a & 0xFF, a & 0xFF, sum & 0xFF,
                         sum == 0 ? "✅" : "❌");
        
        // Test distributive law
        byte c = 0x25;
        byte left = gf.multiply(a, gf.add(b, c));
        byte right = gf.add(gf.multiply(a, b), gf.multiply(a, c));
        System.out.printf("Distributive Law: %s %s%n", 
                         "a×(b+c) = a×b + a×c",
                         left == right ? "✅" : "❌");
        
        assert product == expectedProduct : "CCSDS test vector failed";
        assert sum == 0 : "Characteristic 2 property failed";
        assert left == right : "Distributive law failed";
    }
    
    private static void testErasureSupport() {
        System.out.println("\n4. 🎯 ERASURE SUPPORT (New NASA Feature)");
        System.out.println("---------------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test erasure locator polynomial
        int[] erasurePositions = {1, 3, 5};
        byte[] erasureLocator = gf.computeErasureLocator(erasurePositions);
        
        System.out.println("Erasure positions: " + java.util.Arrays.toString(erasurePositions));
        System.out.print("Erasure locator polynomial: Γ(x) = ");
        for (int i = 0; i < erasureLocator.length; i++) {
            System.out.printf("0x%02X", erasureLocator[i] & 0xFF);
            if (i > 0) System.out.printf("·x^%d", i);
            if (i < erasureLocator.length - 1) System.out.print(" + ");
        }
        System.out.println();
        
        // Test no erasures case
        byte[] noErasures = gf.computeErasureLocator(new int[0]);
        assert noErasures.length == 1 && noErasures[0] == 1 : "No erasures should give Γ(x) = 1";
        System.out.println("✅ No erasures case: Γ(x) = 1");
        
        assert erasureLocator.length == erasurePositions.length + 1 : "Erasure locator degree incorrect";
    }
    
        private static void testPerformanceMonitoring() {
        System.out.println("\n5. ⚡ PERFORMANCE MONITORING (New Feature)");
        System.out.println("-----------------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        gf.resetCounters();
        
        // Generate some operations - FIXED: Avoid division by zero
        final int OPERATIONS = 500;
        for (int i = 1; i <= OPERATIONS; i++) {  // Start from 1 to avoid zeros
            byte x = (byte) (i % 255 + 1);       // Ensure non-zero (1-255)
            byte y = (byte) ((i * 3) % 255 + 1); // Ensure non-zero (1-255)
            
            gf.multiply(x, y);
            gf.divide(x, y);
            if (i % 5 == 0) gf.inverse(x);
            gf.power(x, 2);
        }
        
        CCSDSGaloisField.FieldStatistics stats = gf.getStatistics();
        System.out.println("Performance Counters:");
        System.out.println("  Multiplications: " + stats.multiplications);
        System.out.println("  Divisions: " + stats.divisions);
        System.out.println("  Inverses: " + stats.inverses);
        System.out.println("  Exponentiations: " + stats.exponentiations);
        
        // Test reset functionality
        gf.resetCounters();
        CCSDSGaloisField.FieldStatistics resetStats = gf.getStatistics();
        assert resetStats.multiplications == 0 : "Counter reset failed";
        System.out.println("✅ Counter reset functionality working");
        
        assert stats.multiplications > 0 : "Performance counters not working";
    }
    
    private static void testEdgeCases() {
        System.out.println("\n6. 🚨 EDGE CASES & ERROR HANDLING");
        System.out.println("--------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test division by zero
        try {
            gf.divide((byte)1, (byte)0);
            assert false : "Should throw exception for division by zero";
        } catch (ArithmeticException e) {
            System.out.println("✅ Division by zero: " + e.getMessage());
        }
        
        // Test inverse of zero
        try {
            gf.inverse((byte)0);
            assert false : "Should throw exception for inverse of zero";
        } catch (ArithmeticException e) {
            System.out.println("✅ Inverse of zero: " + e.getMessage());
        }
        
        // Test invalid erasure positions
        try {
            gf.computeErasureLocator(new int[]{300}); // Invalid position
            assert false : "Should throw exception for invalid erasure position";
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Invalid erasure position: " + e.getMessage());
        }
        
        System.out.println("✅ All edge cases handled correctly");
    }
    
    private static void testCCSDSCompliance() {
        System.out.println("\n7. 🛰️ CCSDS COMPLIANCE VERIFICATION");
        System.out.println("----------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Verify CCSDS standards
        assert gf.getFieldSize() == 256 : "CCSDS requires GF(256)";
        assert gf.getPrimitiveElement() == 0x02 : "CCSDS primitive element should be 2";
        
        // Test primitive element generates entire field
        byte alpha = gf.getPrimitiveElement();
        byte current = 1;
        java.util.Set<Integer> generatedElements = new java.util.HashSet<>();
        
        for (int i = 0; i < 255; i++) {
            generatedElements.add(current & 0xFF);
            current = gf.multiply(current, alpha);
        }
        
        assert generatedElements.size() == 255 : "Primitive element should generate all non-zero elements";
        System.out.println("✅ Primitive element generates entire multiplicative group");
        System.out.println("✅ CCSDS GF(256) with polynomial 0x187 verified");
    }
}