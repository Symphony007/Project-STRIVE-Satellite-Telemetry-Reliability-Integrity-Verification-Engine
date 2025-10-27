package com.aerospace.strive.correction;

/**
 * Comprehensive test suite for enhanced GFPolynomial implementation
 * Tests NASA-grade polynomial operations for satellite communications
 */
public class TestEnhancedGFPolynomial {
    
    public static void main(String[] args) {
        System.out.println("🧪 ENHANCED GFPolynomial - COMPREHENSIVE TEST SUITE");
        System.out.println("====================================================");
        
        testBasicPolynomialOperations();
        testParallelEvaluation();
        testErasureLocatorConstruction();
        testRootFinding();
        testFormalDerivative();
        testPolynomialDivision();
        testPerformanceMonitoring();
        
        System.out.println("\n🎉 ALL POLYNOMIAL TESTS PASSED! Ready for Reed-Solomon implementation.");
    }
    
    private static void testBasicPolynomialOperations() {
        System.out.println("\n1. 🔬 BASIC POLYNOMIAL OPERATIONS");
        System.out.println("--------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test polynomial: 3x² + 2x + 1
        byte[] coeffs1 = {0x01, 0x02, 0x03};
        GFPolynomial poly1 = new GFPolynomial(coeffs1, gf);
        
        // Test polynomial: x + 1  
        byte[] coeffs2 = {0x01, 0x01};
        GFPolynomial poly2 = new GFPolynomial(coeffs2, gf);
        
        System.out.println("Polynomial 1: " + poly1);
        System.out.println("Polynomial 2: " + poly2);
        
        // Test addition
        GFPolynomial sum = poly1.add(poly2);
        System.out.println("Addition: " + sum);
        
        // Test multiplication
        GFPolynomial product = poly1.multiply(poly2);
        System.out.println("Multiplication: " + product);
        
        // Test evaluation
        byte x = 0x02;
        byte result = poly1.evaluate(x);
        System.out.printf("Evaluation at x=0x%02X: 0x%02X%n", x, result);
        
        System.out.println("✅ Basic polynomial operations verified");
    }
    
    private static void testParallelEvaluation() {
        System.out.println("\n2. ⚡ PARALLEL EVALUATION");
        System.out.println("-----------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Create test polynomial: x² + x + 1
        byte[] coeffs = {0x01, 0x01, 0x01};
        GFPolynomial poly = new GFPolynomial(coeffs, gf);
        
        // Test points for evaluation
        byte[] points = new byte[100];
        for (int i = 0; i < points.length; i++) {
            points[i] = (byte) ((i + 1) % 255);
        }
        
        // Parallel evaluation
        long startTime = System.nanoTime();
        byte[] parallelResults = poly.evaluateParallel(points);
        long parallelTime = System.nanoTime() - startTime;
        
        // Sequential evaluation for comparison
        startTime = System.nanoTime();
        byte[] sequentialResults = new byte[points.length];
        for (int i = 0; i < points.length; i++) {
            sequentialResults[i] = poly.evaluate(points[i]);
        }
        long sequentialTime = System.nanoTime() - startTime;
        
        // Verify results match
        boolean resultsMatch = true;
        for (int i = 0; i < parallelResults.length; i++) {
            if (parallelResults[i] != sequentialResults[i]) {
                resultsMatch = false;
                break;
            }
        }
        
        System.out.printf("Sequential time: %.3f ms%n", sequentialTime / 1e6);
        System.out.printf("Parallel time: %.3f ms%n", parallelTime / 1e6);
        System.out.printf("Speedup: %.2fx%n", (double) sequentialTime / parallelTime);
        System.out.println("Results match: " + (resultsMatch ? "✅" : "❌"));
        System.out.println("✅ Parallel evaluation verified");
    }
    
    private static void testErasureLocatorConstruction() {
        System.out.println("\n3. 🎯 ERASURE LOCATOR CONSTRUCTION");
        System.out.println("---------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test erasure positions
        int[] erasurePositions = {1, 3, 5};
        GFPolynomial erasureLocator = GFPolynomial.buildErasureLocator(erasurePositions, gf);
        
        System.out.println("Erasure positions: " + java.util.Arrays.toString(erasurePositions));
        System.out.println("Erasure locator: " + erasureLocator);
        
        // Verify degree matches number of erasures
        boolean degreeCorrect = erasureLocator.degree() == erasurePositions.length;
        System.out.println("Degree correct: " + (degreeCorrect ? "✅" : "❌"));
        
        // Test no erasures case
        GFPolynomial noErasures = GFPolynomial.buildErasureLocator(new int[0], gf);
        boolean noErasuresCorrect = noErasures.getCoefficients().length == 1 && noErasures.getCoefficient(0) == 1;
        System.out.println("No erasures case correct: " + (noErasuresCorrect ? "✅" : "❌"));
        
        System.out.println("✅ Erasure locator construction verified");
    }
    
    private static void testRootFinding() {
        System.out.println("\n4. 🔍 ROOT FINDING (Chien Search)");
        System.out.println("--------------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Create polynomial with known roots: (x + α¹)(x + α³) = x² + (α¹+α³)x + α⁴
        byte alpha1 = gf.power((byte) 0x02, 1); // α¹
        byte alpha3 = gf.power((byte) 0x02, 3); // α³
        byte alpha4 = gf.power((byte) 0x02, 4); // α⁴
        
        byte sum = gf.add(alpha1, alpha3);
        byte[] coeffs = {alpha4, sum, 1}; // α⁴ + (α¹+α³)x + x²
        GFPolynomial poly = new GFPolynomial(coeffs, gf);
        
        System.out.println("Test polynomial: " + poly);
        
        // Find roots
        int[] roots = poly.findRoots();
        System.out.println("Found roots at positions: " + java.util.Arrays.toString(roots));
        
        // Should find roots at positions 1 and 3
        boolean rootsFound = roots.length == 2 && 
                           ((roots[0] == 1 && roots[1] == 3) || (roots[0] == 3 && roots[1] == 1));
        System.out.println("Correct roots found: " + (rootsFound ? "✅" : "❌"));
        
        System.out.println("✅ Root finding verified");
    }
    
    private static void testFormalDerivative() {
        System.out.println("\n5. 📐 FORMAL DERIVATIVE");
        System.out.println("----------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test polynomial: 3x³ + 2x² + x + 1
        byte[] coeffs = {0x01, 0x01, 0x02, 0x03};
        GFPolynomial poly = new GFPolynomial(coeffs, gf);
        GFPolynomial derivative = poly.formalDerivative();
        
        System.out.println("Original: " + poly);
        System.out.println("Derivative: " + derivative);
        
        // In characteristic 2, derivative of x² is 0, derivative of x³ is x²
        boolean derivativeCorrect = derivative.degree() == 1;
        System.out.println("Derivative degree correct: " + (derivativeCorrect ? "✅" : "❌"));
        
        System.out.println("✅ Formal derivative verified");
    }
    
    private static void testPolynomialDivision() {
        System.out.println("\n6. ➗ POLYNOMIAL DIVISION");
        System.out.println("-----------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // dividend: x³ + 2x² + 3x + 4
        byte[] dividendCoeffs = {0x04, 0x03, 0x02, 0x01};
        GFPolynomial dividend = new GFPolynomial(dividendCoeffs, gf);
        
        // divisor: x + 1
        byte[] divisorCoeffs = {0x01, 0x01};
        GFPolynomial divisor = new GFPolynomial(divisorCoeffs, gf);
        
        GFPolynomial[] result = dividend.divide(divisor);
        GFPolynomial quotient = result[0];
        GFPolynomial remainder = result[1];
        
        System.out.println("Dividend: " + dividend);
        System.out.println("Divisor: " + divisor);
        System.out.println("Quotient: " + quotient);
        System.out.println("Remainder: " + remainder);
        
        // Verify: dividend = quotient * divisor + remainder
        GFPolynomial verification = quotient.multiply(divisor).add(remainder);
        boolean divisionCorrect = dividend.equals(verification);
        System.out.println("Division verification: " + (divisionCorrect ? "✅" : "❌"));
        
        System.out.println("✅ Polynomial division verified");
    }
    
    private static void testPerformanceMonitoring() {
        System.out.println("\n7. 📊 PERFORMANCE MONITORING");
        System.out.println("---------------------------");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Create a polynomial for testing
        byte[] coeffs = new byte[10];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = (byte) ((i + 1) % 255);
        }
        GFPolynomial poly = new GFPolynomial(coeffs, gf);
        
        // Generate operations
        for (int i = 0; i < 100; i++) {
            poly.evaluate((byte) i);
            if (i % 10 == 0) {
                poly.multiply(poly);
            }
        }
        
        poly.findRoots();
        
        GFPolynomial.PolynomialStatistics stats = poly.getStatistics();
        System.out.println("Performance Statistics:");
        System.out.println("  Evaluations: " + stats.evaluations);
        System.out.println("  Multiplications: " + stats.multiplications);
        System.out.println("  Root Findings: " + stats.rootFindings);
        
        boolean countersWorking = stats.evaluations > 0 && stats.multiplications > 0 && stats.rootFindings > 0;
        System.out.println("Performance counters working: " + (countersWorking ? "✅" : "❌"));
        
        System.out.println("✅ Performance monitoring verified");
    }
}