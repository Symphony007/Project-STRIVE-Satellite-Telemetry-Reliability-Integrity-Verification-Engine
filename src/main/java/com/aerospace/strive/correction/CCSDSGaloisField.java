package com.aerospace.strive.correction;

/**
 * CCSDS-compliant Galois Field GF(256) implementation
 * Primitive polynomial: x⁸ + x⁷ + x² + x + 1 (0x187)
 * Used by NASA, ESA, and other space agencies
 */
public class CCSDSGaloisField {
    // CCSDS Standard primitive polynomial
    private static final int PRIMITIVE = 0x187; // x⁸ + x⁷ + x² + x + 1
    private static final int FIELD_SIZE = 256;
    private static final int ALPHA = 0x02; // Primitive element α = 2
    
    private final byte[] expTable; // Exponential table: α^i
    private final byte[] logTable; // Logarithm table: log_α(x)
    
    public CCSDSGaloisField() {
        this.expTable = new byte[FIELD_SIZE * 2]; // Double size for multiplication
        this.logTable = new byte[FIELD_SIZE];
        initializeTables();
        System.out.println("🛰️  CCSDS Galois Field GF(256) initialized with polynomial 0x" + 
                         Integer.toHexString(PRIMITIVE).toUpperCase());
    }
    
    private void initializeTables() {
        // Build exponential and logarithm tables
        int value = 1;
        for (int i = 0; i < FIELD_SIZE; i++) {
            expTable[i] = (byte) value;
            expTable[i + FIELD_SIZE] = (byte) value; // Double table for easy multiplication
            logTable[value] = (byte) i;
            
            value <<= 1; // Multiply by α (2)
            if ((value & FIELD_SIZE) != 0) {
                value ^= PRIMITIVE; // Reduce modulo primitive polynomial
            }
        }
        logTable[0] = (byte) 0; // log(0) is undefined, but we set to 0
    }
    
    /**
     * Multiply two numbers in GF(256)
     */
    public byte multiply(byte a, byte b) {
        if (a == 0 || b == 0) return 0;
        int aInt = a & 0xFF;
        int bInt = b & 0xFF;
        int logSum = (logTable[aInt] & 0xFF) + (logTable[bInt] & 0xFF);
        return expTable[logSum];
    }
    
    /**
     * Divide a by b in GF(256)
     */
    public byte divide(byte a, byte b) {
        if (b == 0) throw new ArithmeticException("Division by zero in GF(256)");
        if (a == 0) return 0;
        int aInt = a & 0xFF;
        int bInt = b & 0xFF;
        int logDiff = (logTable[aInt] & 0xFF) - (logTable[bInt] & 0xFF);
        if (logDiff < 0) logDiff += FIELD_SIZE - 1;
        return expTable[logDiff];
    }
    
    /**
     * Compute a raised to power b in GF(256)
     */
    public byte power(byte a, int exponent) {
        if (exponent == 0) return 1;
        if (a == 0) return 0;
        
        int aInt = a & 0xFF;
        int logResult = (logTable[aInt] & 0xFF) * exponent;
        return expTable[logResult % (FIELD_SIZE - 1)];
    }
    
    /**
     * Compute multiplicative inverse in GF(256)
     */
    public byte inverse(byte a) {
        if (a == 0) throw new ArithmeticException("Zero has no multiplicative inverse");
        int aInt = a & 0xFF;
        int logInverse = (FIELD_SIZE - 1) - (logTable[aInt] & 0xFF);
        return expTable[logInverse];
    }
    
    /**
     * Verify field correctness with test cases
     */
    public static void testField() {
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test: α * α^-1 should equal 1
        byte alpha = 0x02;
        byte alphaInverse = gf.inverse(alpha);
        byte result = gf.multiply(alpha, alphaInverse);
        
        System.out.println("🧪 CCSDS Field Verification:");
        System.out.println("α * α^-1 = " + (result == 1 ? "✅ PASS" : "❌ FAIL"));
        System.out.println("Field polynomial: 0x" + Integer.toHexString(PRIMITIVE).toUpperCase());
    }
    
    // Getters for tables (used by polynomial operations)
    public byte[] getExpTable() { return expTable.clone(); }
    public byte[] getLogTable() { return logTable.clone(); }
    public int getFieldSize() { return FIELD_SIZE; }
}