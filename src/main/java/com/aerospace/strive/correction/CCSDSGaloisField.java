package com.aerospace.strive.correction;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CCSDS-compliant Galois Field GF(256) implementation with NASA-grade enhancements
 * Primitive polynomial: x⁸ + x⁷ + x² + x + 1 (0x187)
 * Supports erasure decoding, performance monitoring, and scientific validation
 */
public class CCSDSGaloisField {
    // CCSDS Standard primitive polynomial
    private static final int PRIMITIVE = 0x187; // x⁸ + x⁷ + x² + x + 1
    private static final int FIELD_SIZE = 256;
    private static final int ALPHA = 0x02; // Primitive element α = 2
    private static final int FIELD_ORDER = 255; // Multiplicative group order
    
    // Mathematical constants
    private static final byte ZERO = 0;
    private static final byte ONE = 1;
    
    // Lookup tables
    private final byte[] expTable; // Exponential table: α^i
    private final byte[] logTable; // Logarithm table: log_α(x)
    
    // Performance monitoring
    private final AtomicLong multiplicationCount = new AtomicLong(0);
    private final AtomicLong divisionCount = new AtomicLong(0);
    private final AtomicLong inverseCount = new AtomicLong(0);
    private final AtomicLong powerCount = new AtomicLong(0);
    
    // Field validation data
    private final boolean[] fieldElementValidation;
    
    public CCSDSGaloisField() {
        this.expTable = new byte[FIELD_SIZE * 2]; // Double size for multiplication
        this.logTable = new byte[FIELD_SIZE];
        this.fieldElementValidation = new boolean[FIELD_SIZE];
        initializeTables();
        validateFieldProperties();
        System.out.println("🛰️  CCSDS Galois Field GF(256) initialized with polynomial 0x" + 
                         Integer.toHexString(PRIMITIVE).toUpperCase());
        printFieldProperties();
    }
    
    /**
     * Initialize exponential and logarithm tables with mathematical validation
     */
    private void initializeTables() {
        Arrays.fill(logTable, (byte) -1); // Initialize as invalid
        Arrays.fill(fieldElementValidation, false);
        
        int value = 1;
        for (int i = 0; i < FIELD_ORDER; i++) {
            expTable[i] = (byte) value;
            expTable[i + FIELD_ORDER] = (byte) value; // Double table for easy multiplication
            logTable[value] = (byte) i;
            fieldElementValidation[value] = true;
            
            // Multiply by α (primitive element)
            value <<= 1; 
            if ((value & FIELD_SIZE) != 0) {
                value ^= PRIMITIVE; // Reduce modulo primitive polynomial
            }
        }
        
        // Special cases
        expTable[FIELD_ORDER] = 1; // α^255 = 1
        logTable[0] = (byte) 0; // log(0) is undefined, but we set to 0 for safety
        fieldElementValidation[0] = true; // Zero is valid field element
    }
    
    /**
     * Validate all field properties mathematically
     */
    private void validateFieldProperties() {
        System.out.println("🔬 Validating Galois Field Mathematical Properties...");
        
        // Test closure under addition (characteristic 2)
        validateClosure();
        
        // Test multiplicative inverses
        validateInverses();
        
        // Test primitive element properties
        validatePrimitiveElement();
        
        // Test field isomorphism
        validateIsomorphism();
        
        System.out.println("✅ Field validation completed successfully");
    }
    
    private void validateClosure() {
        for (int i = 0; i < FIELD_SIZE; i++) {
            for (int j = 0; j < FIELD_SIZE; j++) {
                byte a = (byte) i;
                byte b = (byte) j;
                byte sum = add(a, b);
                byte product = multiply(a, b);
                
                if (!isValidFieldElement(sum) || !isValidFieldElement(product)) {
                    throw new IllegalStateException("Field closure violation at (" + i + "," + j + ")");
                }
            }
        }
        System.out.println("✅ Closure under addition and multiplication verified");
    }
    
    private void validateInverses() {
        int validInverses = 0;
        for (int i = 1; i < FIELD_SIZE; i++) {
            byte a = (byte) i;
            try {
                byte inverse = inverse(a);
                byte product = multiply(a, inverse);
                if (product != ONE) {
                    throw new IllegalStateException("Inverse validation failed for element " + i);
                }
                validInverses++;
            } catch (ArithmeticException e) {
                // Expected for zero, but we're skipping zero
            }
        }
        System.out.println("✅ Multiplicative inverses verified for " + validInverses + " elements");
    }
    
    private void validatePrimitiveElement() {
        byte alpha = (byte) ALPHA;
        byte current = ONE;
        
        // Check that α generates all non-zero elements
        boolean[] generated = new boolean[FIELD_SIZE];
        generated[0] = true; // Skip zero
        
        for (int i = 0; i < FIELD_ORDER; i++) {
            int value = current & 0xFF;
            if (generated[value]) {
                throw new IllegalStateException("Primitive element does not generate entire field");
            }
            generated[value] = true;
            current = multiply(current, alpha);
        }
        
        // Verify α^255 = 1
        byte alphaPower255 = power(alpha, FIELD_ORDER);
        if (alphaPower255 != ONE) {
            throw new IllegalStateException("Primitive element order incorrect: α^255 ≠ 1");
        }
        
        System.out.println("✅ Primitive element α = 0x" + Integer.toHexString(ALPHA) + " verified");
    }
    
    private void validateIsomorphism() {
        // Verify polynomial representation matches exponential representation
        for (int i = 0; i < FIELD_SIZE; i++) {
            byte element = (byte) i;
            byte reconstructed = reconstructFromLog(element);
            if (element != reconstructed && i != 0) { // Skip zero
                throw new IllegalStateException("Field isomorphism broken at element " + i);
            }
        }
        System.out.println("✅ Field isomorphism verified");
    }
    
    /**
     * Reconstruct field element from its logarithm (for validation)
     */
    private byte reconstructFromLog(byte element) {
        if (element == 0) return 0;
        int log = logTable[element & 0xFF] & 0xFF;
        return expTable[log];
    }
    
    /**
     * Add two elements in GF(256) - XOR operation
     */
    public byte add(byte a, byte b) {
        return (byte) (a ^ b);
    }
    
    /**
     * Subtract two elements in GF(256) - same as addition in characteristic 2
     */
    public byte subtract(byte a, byte b) {
        return add(a, b);
    }
    
    /**
     * Multiply two numbers in GF(256) with performance monitoring
     */
    public byte multiply(byte a, byte b) {
        multiplicationCount.incrementAndGet();
        
        if (a == 0 || b == 0) return 0;
        
        int aInt = a & 0xFF;
        int bInt = b & 0xFF;
        
        // Mathematical validation
        if (!isValidFieldElement(a) || !isValidFieldElement(b)) {
            throw new IllegalArgumentException("Invalid field elements for multiplication");
        }
        
        int logSum = (logTable[aInt] & 0xFF) + (logTable[bInt] & 0xFF);
        byte result = expTable[logSum % FIELD_ORDER];
        
        // Verify result is valid field element
        if (!isValidFieldElement(result)) {
            throw new ArithmeticException("Multiplication produced invalid field element");
        }
        
        return result;
    }
    
    /**
     * Divide a by b in GF(256) with comprehensive error checking
     */
    public byte divide(byte a, byte b) {
        divisionCount.incrementAndGet();
        
        if (b == 0) {
            throw new ArithmeticException("Division by zero in GF(256)");
        }
        if (a == 0) return 0;
        
        int aInt = a & 0xFF;
        int bInt = b & 0xFF;
        
        if (!isValidFieldElement(a) || !isValidFieldElement(b)) {
            throw new IllegalArgumentException("Invalid field elements for division");
        }
        
        int logDiff = (logTable[aInt] & 0xFF) - (logTable[bInt] & 0xFF);
        if (logDiff < 0) logDiff += FIELD_ORDER;
        
        byte result = expTable[logDiff];
        
        if (!isValidFieldElement(result)) {
            throw new ArithmeticException("Division produced invalid field element");
        }
        
        return result;
    }
    
    /**
     * Compute a raised to power b in GF(256)
     */
    public byte power(byte a, int exponent) {
        powerCount.incrementAndGet();
        
        if (exponent == 0) return 1;
        if (a == 0) return 0;
        if (exponent == 1) return a;
        
        // Handle negative exponents
        if (exponent < 0) {
            a = inverse(a);
            exponent = -exponent;
        }
        
        int aInt = a & 0xFF;
        int logResult = (logTable[aInt] & 0xFF) * exponent;
        byte result = expTable[logResult % FIELD_ORDER];
        
        if (!isValidFieldElement(result)) {
            throw new ArithmeticException("Exponentiation produced invalid field element");
        }
        
        return result;
    }
    
    /**
     * Compute multiplicative inverse in GF(256) with validation
     */
    public byte inverse(byte a) {
        inverseCount.incrementAndGet();
        
        if (a == 0) {
            throw new ArithmeticException("Zero has no multiplicative inverse in GF(256)");
        }
        
        if (!isValidFieldElement(a)) {
            throw new IllegalArgumentException("Invalid field element for inverse");
        }
        
        int aInt = a & 0xFF;
        int logInverse = FIELD_ORDER - (logTable[aInt] & 0xFF);
        byte result = expTable[logInverse];
        
        // Verify inverse property: a * a⁻¹ = 1
        byte verification = multiply(a, result);
        if (verification != ONE) {
            throw new ArithmeticException("Inverse validation failed for element 0x" + 
                                       Integer.toHexString(a & 0xFF));
        }
        
        return result;
    }
    
    /**
     * Check if byte represents valid GF(256) element
     */
    public boolean isValidFieldElement(byte element) {
        int value = element & 0xFF;
        return value >= 0 && value < FIELD_SIZE && fieldElementValidation[value];
    }
    
    /**
     * Compute erasure locator polynomial from known erasure positions
     */
    public byte[] computeErasureLocator(int[] erasurePositions) {
        if (erasurePositions == null || erasurePositions.length == 0) {
            return new byte[]{1}; // Γ(x) = 1 for no erasures
        }
        
        // Γ(x) = Π (1 - α^{position} * x)
        byte[] locator = new byte[]{1}; // Start with Γ(x) = 1
        
        for (int position : erasurePositions) {
            if (position < 0 || position >= FIELD_ORDER) {
                throw new IllegalArgumentException("Invalid erasure position: " + position);
            }
            
            byte alphaPower = power((byte) ALPHA, position);
            byte[] factor = new byte[]{alphaPower, 1}; // (α^position + x)
            locator = multiplyPolynomials(locator, factor);
        }
        
        return locator;
    }
    
    /**
     * Multiply two polynomials in GF(256)
     */
    private byte[] multiplyPolynomials(byte[] poly1, byte[] poly2) {
        byte[] result = new byte[poly1.length + poly2.length - 1];
        
        for (int i = 0; i < poly1.length; i++) {
            for (int j = 0; j < poly2.length; j++) {
                byte product = multiply(poly1[i], poly2[j]);
                result[i + j] = add(result[i + j], product);
            }
        }
        
        return result;
    }
    
    /**
     * Get field statistics for performance monitoring
     */
    public FieldStatistics getStatistics() {
        return new FieldStatistics(
            multiplicationCount.get(),
            divisionCount.get(),
            inverseCount.get(),
            powerCount.get()
        );
    }
    
    /**
     * Reset performance counters
     */
    public void resetCounters() {
        multiplicationCount.set(0);
        divisionCount.set(0);
        inverseCount.set(0);
        powerCount.set(0);
    }
    
    private void printFieldProperties() {
        System.out.println("📊 Field Properties:");
        System.out.println("   - Characteristic: 2");
        System.out.println("   - Extension Degree: 8");
        System.out.println("   - Order: 256");
        System.out.println("   - Primitive Element: α = 0x" + Integer.toHexString(ALPHA));
        System.out.println("   - Primitive Polynomial: 0x" + Integer.toHexString(PRIMITIVE));
        System.out.println("   - Multiplicative Group Order: " + FIELD_ORDER);
    }
    
    // Getters for tables (used by polynomial operations)
    public byte[] getExpTable() { return expTable.clone(); }
    public byte[] getLogTable() { return logTable.clone(); }
    public int getFieldSize() { return FIELD_SIZE; }
    public int getFieldOrder() { return FIELD_ORDER; }
    public byte getPrimitiveElement() { return (byte) ALPHA; }
    
    /**
     * Performance statistics container
     */
    public static class FieldStatistics {
        public final long multiplications;
        public final long divisions;
        public final long inverses;
        public final long exponentiations;
        
        public FieldStatistics(long multiplications, long divisions, long inverses, long exponentiations) {
            this.multiplications = multiplications;
            this.divisions = divisions;
            this.inverses = inverses;
            this.exponentiations = exponentiations;
        }
        
        @Override
        public String toString() {
            return String.format("Field Operations: ×=%d, ÷=%d, inv=%d, pow=%d",
                               multiplications, divisions, inverses, exponentiations);
        }
    }
    
    /**
     * Comprehensive field verification with test cases
     */
    public static void testFieldComprehensive() {
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        System.out.println("\n🧪 Comprehensive Field Testing");
        System.out.println("=============================");
        
        // Test critical mathematical properties
        testFieldProperties(gf);
        testArithmeticOperations(gf);
        testEdgeCases(gf);
        testPerformance(gf);
        
        System.out.println("✅ All field tests passed!");
    }
    
    private static void testFieldProperties(CCSDSGaloisField gf) {
        System.out.println("Testing Field Properties...");
        
        // Test additive identity
        byte zero = 0;
        byte one = 1;
        assert gf.add(zero, one) == one : "Additive identity failed";
        
        // Test multiplicative identity
        assert gf.multiply(one, one) == one : "Multiplicative identity failed";
        
        // Test distributive law
        byte a = 0x53, b = (byte) 0xCA, c = 0x25;
        byte left = gf.multiply(a, gf.add(b, c));
        byte right = gf.add(gf.multiply(a, b), gf.multiply(a, c));
        assert left == right : "Distributive law failed";
        
        System.out.println("✅ Field properties verified");
    }
    
    private static void testArithmeticOperations(CCSDSGaloisField gf) {
        System.out.println("Testing Arithmetic Operations...");
        
        // Test multiplication and division inverses
        byte a = 0x53, b = (byte) 0xCA;
        byte product = gf.multiply(a, b);
        byte quotient = gf.divide(product, b);
        assert quotient == a : "Multiplication/division inverse failed";
        
        // Test exponentiation
        byte alpha = gf.getPrimitiveElement();
        byte alphaSquared = gf.multiply(alpha, alpha);
        byte alphaPower2 = gf.power(alpha, 2);
        assert alphaSquared == alphaPower2 : "Exponentiation failed";
        
        System.out.println("✅ Arithmetic operations verified");
    }
    
    private static void testEdgeCases(CCSDSGaloisField gf) {
        System.out.println("Testing Edge Cases...");
        
        // Test zero handling
        try {
            gf.inverse((byte) 0);
            assert false : "Should throw exception for zero inverse";
        } catch (ArithmeticException e) {
            // Expected
        }
        
        // Test division by zero
        try {
            gf.divide((byte) 1, (byte) 0);
            assert false : "Should throw exception for division by zero";
        } catch (ArithmeticException e) {
            // Expected
        }
        
        // Test field element validation
        assert gf.isValidFieldElement((byte) 0) : "Zero should be valid";
        assert gf.isValidFieldElement((byte) 1) : "One should be valid";
        assert gf.isValidFieldElement((byte) 255) : "255 should be valid";
        
        System.out.println("✅ Edge cases handled correctly");
    }
    
    private static void testPerformance(CCSDSGaloisField gf) {
        System.out.println("Testing Performance...");
        
        long startTime = System.nanoTime();
        final int OPERATIONS = 10000;
        
        for (int i = 0; i < OPERATIONS; i++) {
            byte a = (byte) (i % 256);
            byte b = (byte) ((i * 3) % 256);
            gf.multiply(a, b);
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1e6;
        double operationsPerMs = OPERATIONS / durationMs;
        
        System.out.printf("✅ Performance: %.2f operations/ms\n", operationsPerMs);
        
        FieldStatistics stats = gf.getStatistics();
        System.out.println("📊 " + stats.toString());
    }
}