package com.aerospace.strive.correction;

import java.util.Arrays;

/**
 * NASA-GRADE BCH CODEC - Bose–Chaudhuri–Hocquenghem Codes
 * 
 * SCIENTIFIC FOUNDATION:
 * - Binary BCH codes for random bit error correction
 * - Cyclic codes based on finite field arithmetic
 * - Strong algebraic structure for guaranteed correction capability
 * 
 * MATHEMATICAL MODELS:
 * - Generator polynomial construction from minimal polynomials
 * - Berlekamp-Massey algorithm for error location
 * - Chien search for error position finding
 * - Finite field GF(2^m) arithmetic
 * 
 * SATELLITE APPLICATIONS:
 * - Telemetry systems with random bit errors
 * - Flash memory error correction in space hardware
 * - Complementary to Reed-Solomon for mixed error types
 */
public class BCHCodec {
    
    // BCH(15,7,2) - Corrects 2 errors in 15-bit codeword, 7 data bits
    private static final int N = 15; // Codeword length
    private static final int K = 7;  // Data length
    private static final int T = 2;  // Error correction capability
    
    // Generator polynomial for BCH(15,7,2): g(x) = x^8 + x^7 + x^6 + x^4 + 1
    private static final int GENERATOR_POLY = 0b111010001; // x^8 + x^7 + x^6 + x^4 + 1
    
    // Finite field GF(16) - primitive polynomial: x^4 + x + 1
    private static final int PRIMITIVE_POLY = 0b10011; // x^4 + x + 1
    private static final int[] GF_EXP = new int[30];   // Exponential table
    private static final int[] GF_LOG = new int[16];   // Logarithm table
    
    static {
        initializeGaloisField();
    }
    
    /**
     * Initialize GF(16) for BCH code calculations
     */
    private static void initializeGaloisField() {
        int value = 1;
        for (int i = 0; i < 15; i++) {
            GF_EXP[i] = value;
            GF_LOG[value] = i;
            value <<= 1;
            if ((value & 16) != 0) {
                value ^= PRIMITIVE_POLY;
            }
        }
        // Double table for multiplication
        for (int i = 15; i < 30; i++) {
            GF_EXP[i] = GF_EXP[i - 15];
        }
    }
    
    /**
     * Multiply two elements in GF(16)
     */
    private static int gfMultiply(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return GF_EXP[GF_LOG[a] + GF_LOG[b]];
    }
    
    /**
     * Add two elements in GF(16) - XOR operation
     */
    private static int gfAdd(int a, int b) {
        return a ^ b;
    }
    
    /**
     * Compute power in GF(16)
     */
    private static int gfPower(int a, int power) {
        if (a == 0) return 0;
        return GF_EXP[(GF_LOG[a] * power) % 15];
    }
    
    /**
     * Encode 7 data bits into 15-bit BCH codeword
     */
    public int encode(int data) {
        if (data >= (1 << K)) {
            throw new IllegalArgumentException("Data exceeds " + K + " bits");
        }
        
        // Multiply data by x^8 (shift left by 8)
        int shifted = data << (N - K);
        
        // Calculate remainder modulo generator polynomial
        int remainder = polynomialModulo(shifted, GENERATOR_POLY, N);
        
        // Codeword = data * x^8 + remainder
        return shifted | remainder;
    }
    
    /**
     * Decode BCH codeword with error correction
     */
    public int decode(int received) {
        if (received >= (1 << N)) {
            throw new IllegalArgumentException("Received word exceeds " + N + " bits");
        }
        
        // Step 1: Calculate syndromes
        int[] syndromes = calculateSyndromes(received);
        
        // Step 2: Check if no errors
        if (allZero(syndromes)) {
            return received >> (N - K); // Return data bits
        }
        
        // Step 3: Find error locator polynomial
        int[] errorLocator = findErrorLocator(syndromes);
        
        // Step 4: Find error positions
        int[] errorPositions = findErrorPositions(errorLocator);
        
        // Step 5: Correct errors
        int corrected = correctErrors(received, errorPositions);
        
        return corrected >> (N - K); // Return corrected data bits
    }
    
    /**
     * Calculate syndromes S1, S2, S3, S4 for BCH decoding
     */
    private int[] calculateSyndromes(int received) {
        int[] syndromes = new int[2 * T];
        
        for (int i = 0; i < 2 * T; i++) {
            syndromes[i] = calculateSyndrome(received, i + 1);
        }
        
        return syndromes;
    }
    
    /**
     * Calculate single syndrome S_i = r(α^i)
     */
    private int calculateSyndrome(int received, int i) {
        int syndrome = 0;
        int alpha_power = GF_EXP[i % 15];
        int current_power = 1;
        
        for (int bit = N - 1; bit >= 0; bit--) {
            if (((received >> bit) & 1) == 1) {
                syndrome = gfAdd(syndrome, current_power);
            }
            current_power = gfMultiply(current_power, alpha_power);
        }
        
        return syndrome;
    }
    
    /**
     * Find error locator polynomial using Berlekamp-Massey algorithm
     */
    private int[] findErrorLocator(int[] syndromes) {
        int[] C = new int[T + 1]; // Error locator polynomial
        int[] B = new int[T + 1]; // Previous polynomial
        C[0] = 1;
        B[0] = 1;
        int L = 0;
        int m = 1;
        int b = 1;
        
        for (int n = 0; n < 2 * T; n++) {
            // Calculate discrepancy
            int d = syndromes[n];
            for (int i = 1; i <= L; i++) {
                d = gfAdd(d, gfMultiply(C[i], syndromes[n - i]));
            }
            
            if (d == 0) {
                m++;
            } else {
                int[] T_poly = Arrays.copyOf(C, C.length);
                
                // Update C(x) = C(x) - (d/b) * x^m * B(x)
                int scale = gfMultiply(d, gfInverse(b));
                
                for (int i = 0; i <= T; i++) {
                    if (i + m <= T) {
                        C[i + m] = gfAdd(C[i + m], gfMultiply(scale, B[i]));
                    }
                }
                
                if (2 * L <= n) {
                    L = n + 1 - L;
                    B = T_poly;
                    b = d;
                    m = 1;
                } else {
                    m++;
                }
            }
        }
        
        return Arrays.copyOf(C, L + 1);
    }
    
    /**
     * Find error positions using Chien search
     */
    private int[] findErrorPositions(int[] errorLocator) {
        int[] positions = new int[T];
        int count = 0;
        
        for (int i = 0; i < N; i++) {
            int sum = errorLocator[0]; // Constant term
            for (int j = 1; j < errorLocator.length; j++) {
                sum = gfAdd(sum, gfMultiply(errorLocator[j], gfPower(GF_EXP[i], j)));
            }
            
            if (sum == 0) {
                positions[count++] = N - 1 - i; // Position from MSB
                if (count >= T) break;
            }
        }
        
        return Arrays.copyOf(positions, count);
    }
    
    /**
     * Correct errors by flipping bits at error positions
     */
    private int correctErrors(int received, int[] errorPositions) {
        int corrected = received;
        for (int pos : errorPositions) {
            corrected ^= (1 << pos); // Flip the erroneous bit
        }
        return corrected;
    }
    
    /**
     * Polynomial modulo operation for encoding
     */
    private int polynomialModulo(int dividend, int divisor, int degree) {
        int divisorDegree = degree - K;
        
        for (int i = degree - 1; i >= divisorDegree; i--) {
            if (((dividend >> i) & 1) == 1) {
                dividend ^= (divisor << (i - divisorDegree));
            }
        }
        
        return dividend & ((1 << divisorDegree) - 1);
    }
    
    /**
     * Multiplicative inverse in GF(16)
     */
    private int gfInverse(int a) {
        if (a == 0) return 0;
        return GF_EXP[15 - GF_LOG[a]];
    }
    
    private boolean allZero(int[] array) {
        for (int value : array) {
            if (value != 0) return false;
        }
        return true;
    }
    
    // Performance monitoring
    private long encodeCount = 0;
    private long decodeCount = 0;
    private long correctedErrors = 0;
    
    /**
     * Test BCH codec with satellite data patterns
     */
    public static void testBCHCodec() {
        BCHCodec bch = new BCHCodec();
        
        System.out.println("\n🔬 BCH(15,7,2) Codec Scientific Testing");
        System.out.println("======================================");
        System.out.println("   - Codeword length: " + N + " bits");
        System.out.println("   - Data length: " + K + " bits");  
        System.out.println("   - Error correction: " + T + " bits");
        System.out.println("   - Generator polynomial: 0x" + Integer.toHexString(GENERATOR_POLY));
        
        // Test with realistic data patterns from satellite telemetry
        int[] testData = {0b1010101, 0b1100110, 0b1001001, 0b1110001};
        
        for (int data : testData) {
            // Encode
            int encoded = bch.encode(data);
            System.out.println("Data: " + Integer.toBinaryString(data) + " → Encoded: " + 
                             String.format("%15s", Integer.toBinaryString(encoded)).replace(' ', '0'));
            
            // Introduce errors (simulate cosmic radiation)
            int errors = introduceErrors(encoded, 2); // 2-bit errors
            int decoded = bch.decode(errors);
            
            boolean success = (data == decoded);
            System.out.println("Errors introduced → Decoded: " + Integer.toBinaryString(decoded) + 
                             " " + (success ? "✅" : "❌"));
        }
    }
    
    /**
     * Introduce random bit errors for testing
     */
    private static int introduceErrors(int codeword, int maxErrors) {
        int erroneous = codeword;
        java.util.Random rand = new java.util.Random();
        int errorCount = 1 + rand.nextInt(maxErrors);
        
        for (int i = 0; i < errorCount; i++) {
            int errorPos = rand.nextInt(N);
            erroneous ^= (1 << errorPos);
        }
        
        return erroneous;
    }
    
    public static void main(String[] args) {
        testBCHCodec();
    }
}