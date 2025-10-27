package com.aerospace.strive.correction;

import java.util.Arrays;

/**
 * CCSDS-compliant Reed-Solomon encoder/decoder for satellite communications
 * FIXED VERSION - with proven implementation
 */
public class CCSDSReedSolomon {
    private final CCSDSGaloisField field;
    private final GFPolynomial generator;
    private final int n; // Codeword length
    private final int k; // Message length  
    private final int t; // Error correction capability
    
    // CCSDS Standard: RS(255,223) can correct 16 symbol errors
    public static final int CCSDS_N = 255;
    public static final int CCSDS_K = 223;
    public static final int CCSDS_T = 16;
    
    // Performance monitoring
    private long decodeOperations = 0;
    private long correctedErrors = 0;
    private long decodeFailures = 0;
    
    // Debug mode
    private boolean debugMode = true;
    
    public CCSDSReedSolomon() {
        this(CCSDS_N, CCSDS_K);
    }
    
    public CCSDSReedSolomon(int n, int k) {
        this.field = new CCSDSGaloisField();
        this.n = n;
        this.k = k;
        this.t = (n - k) / 2;
        this.generator = buildGeneratorPolynomial();
        
        System.out.printf("ðŸ›°ï¸  CCSDS Reed-Solomon RS(%d,%d) initialized - can correct %d symbol errors%n", 
                         n, k, t);
    }
    
    /**
     * Build generator polynomial: g(x) = (x - Î±)(x - Î±Â²)...(x - Î±^(2t))
     */
    private GFPolynomial buildGeneratorPolynomial() {
        // Start with g(x) = 1
        GFPolynomial gen = new GFPolynomial(new byte[]{1}, field);
        
        // Multiply by (x - Î±^i) for i = 1 to 2t
        for (int i = 1; i <= 2 * t; i++) {
            byte alphaPower = field.power((byte) 2, i); // Î±^i
            GFPolynomial factor = new GFPolynomial(new byte[]{alphaPower, 1}, field);
            gen = gen.multiply(factor);
        }
        
        return gen;
    }
    
    /**
     * FIXED Encode message with systematic Reed-Solomon coding
     * Output: [message][parity] 
     */
    public byte[] encode(byte[] message) {
        if (message.length > k) {
            throw new IllegalArgumentException(
                String.format("Message too long: %d > %d", message.length, k));
        }
        
        // For shortened codes, pad message with zeros
        byte[] paddedMessage = Arrays.copyOf(message, k);
        
        // Create message polynomial: m(x)
        GFPolynomial messagePoly = new GFPolynomial(paddedMessage, field);
        
        // Multiply by x^(n-k) to shift: m(x) * x^(n-k)
        byte[] shiftedCoeffs = new byte[paddedMessage.length + (n - k)];
        System.arraycopy(paddedMessage, 0, shiftedCoeffs, n - k, paddedMessage.length);
        GFPolynomial shiftedPoly = new GFPolynomial(shiftedCoeffs, field);
        
        // Calculate remainder using proper polynomial division
        GFPolynomial remainder = computeRemainder(shiftedPoly, generator);
        
        // Systematic codeword: [message][parity]
        byte[] codeword = Arrays.copyOf(paddedMessage, n);
        byte[] remainderCoeffs = remainder.getCoefficients();
        
        // Copy remainder (parity) to end of codeword
        for (int i = 0; i < remainderCoeffs.length && i < (n - k); i++) {
            codeword[k + i] = remainderCoeffs[i];
        }
        
        if (debugMode) {
            System.out.println("ðŸ”§ ENCODER DEBUG:");
            System.out.println("  Message: " + Arrays.toString(message));
            System.out.println("  Remainder: " + Arrays.toString(remainderCoeffs));
            System.out.println("  Codeword: " + Arrays.toString(codeword));
            
            // Verify encoding by checking syndromes
            byte[] syndromes = computeSyndromes(codeword);
            boolean allZero = true;
            for (byte s : syndromes) {
                if (s != 0) allZero = false;
            }
            System.out.println("  Syndromes all zero: " + (allZero ? "✅" : "❌"));
        }
        
        return codeword;
    }
    
    /**
     * Compute polynomial remainder using proper long division
     */
    private GFPolynomial computeRemainder(GFPolynomial dividend, GFPolynomial divisor) {
        byte[] remainder = dividend.getCoefficients().clone();
        byte[] divisorCoeffs = divisor.getCoefficients();
        
        int divisorDegree = divisor.degree();
        byte divisorLead = divisorCoeffs[divisorDegree];
        
        // Perform polynomial long division
        for (int i = remainder.length - 1; i >= divisorDegree; i--) {
            if (remainder[i] != 0) {
                byte scale = field.divide(remainder[i], divisorLead);
                
                // Subtract scaled divisor from remainder
                for (int j = 0; j <= divisorDegree; j++) {
                    int pos = i - divisorDegree + j;
                    if (pos < remainder.length) {
                        byte product = field.multiply(divisorCoeffs[j], scale);
                        remainder[pos] = field.add(remainder[pos], product);
                    }
                }
            }
        }
        
        // Return remainder (lower degree terms)
        return new GFPolynomial(Arrays.copyOf(remainder, divisorDegree), field);
    }
    
    /**
     * Complete Reed-Solomon decoding with error correction
     */
    public byte[] decode(byte[] received, int[] erasurePositions) {
        decodeOperations++;
        
        if (debugMode) {
            System.out.println("\nðŸ” DECODER DEBUG - START");
            System.out.println("  Received: " + Arrays.toString(received));
            if (erasurePositions != null) {
                System.out.println("  Erasures: " + Arrays.toString(erasurePositions));
            }
        }
        
        if (received.length != n) {
            throw new IllegalArgumentException(
                String.format("Received codeword length %d != expected %d", received.length, n));
        }
        
        // Step 1: Calculate syndromes
        byte[] syndromes = computeSyndromes(received);
        if (debugMode) {
            System.out.println("  Step 1 - Syndromes: " + Arrays.toString(syndromes));
        }
        
        // Step 2: Check if no errors (all syndromes zero)
        if (allZero(syndromes)) {
            if (debugMode) System.out.println("  Step 2 - No errors detected, returning original");
            return Arrays.copyOf(received, k);
        }
        
        // Step 3: Compute error locator polynomial
        GFPolynomial errorLocator;
        if (erasurePositions != null && erasurePositions.length > 0) {
            if (debugMode) System.out.println("  Step 3 - Computing error locator WITH erasures");
            errorLocator = computeErrorLocatorWithErasures(syndromes, erasurePositions);
        } else {
            if (debugMode) System.out.println("  Step 3 - Computing error locator (no erasures)");
            errorLocator = computeErrorLocator(syndromes);
        }
        
        if (debugMode) {
            System.out.println("  Error locator polynomial: " + errorLocator);
            System.out.println("  Error locator degree: " + errorLocator.degree());
        }
        
        // Step 4: Find error positions using Chien search
        int[] errorPositions = findErrorPositions(errorLocator);
        if (debugMode) {
            System.out.println("  Step 4 - Error positions found: " + Arrays.toString(errorPositions));
        }
        
        // Step 5: Validate error positions are within bounds
        errorPositions = validateErrorPositions(errorPositions);
        if (errorPositions == null) {
            if (debugMode) System.out.println("  Step 5 - Invalid error positions, decoding failed");
            decodeFailures++;
            return null;
        }
        
        // Step 6: Check if errors are within correction capability
        int totalErrors = errorPositions.length + (erasurePositions != null ? erasurePositions.length : 0);
        if (debugMode) {
            System.out.println("  Step 6 - Total errors: " + totalErrors + " (limit: " + t + ")");
        }
        
        if (totalErrors > t) {
            if (debugMode) System.out.println("  Step 6 - Too many errors, uncorrectable");
            decodeFailures++;
            return null;
        }
        
        // Step 7: Compute error magnitudes using Forney algorithm
        byte[] errorMagnitudes = computeErrorMagnitudes(syndromes, errorLocator, errorPositions);
        if (debugMode) {
            System.out.println("  Step 7 - Error magnitudes: " + Arrays.toString(errorMagnitudes));
        }
        
        // Step 8: Correct errors
        byte[] corrected = correctErrors(received, errorPositions, errorMagnitudes);
        if (debugMode) {
            System.out.println("  Step 8 - Corrected codeword: " + Arrays.toString(corrected));
        }
        
        int totalErrorsCorrected = (errorPositions != null ? errorPositions.length : 0);
        correctedErrors += totalErrorsCorrected;
        
        if (debugMode) {
            System.out.println("ðŸ” DECODER DEBUG - END");
            System.out.printf("  Corrected %d errors, %d erasures%n", 
                             totalErrorsCorrected, 
                             erasurePositions != null ? erasurePositions.length : 0);
        }
        
        return Arrays.copyOf(corrected, k);
    }
    
    /**
     * PROVEN Berlekamp-Massey algorithm implementation
     */
    private GFPolynomial computeErrorLocator(byte[] syndromes) {
        if (debugMode) {
            System.out.println("    BM Algorithm START");
        }
        
        // Standard BM initialization
        GFPolynomial C = new GFPolynomial(new byte[]{1}, field); // Error locator
        GFPolynomial B = new GFPolynomial(new byte[]{1}, field); // Previous locator
        int L = 0;
        int m = 1;
        byte b = 1;
        
        for (int n = 0; n < syndromes.length; n++) {
            // Calculate discrepancy Î”
            byte d = syndromes[n];
            for (int i = 1; i <= L; i++) {
                d = field.add(d, field.multiply(C.getCoefficient(i), syndromes[n - i]));
            }
            
            if (debugMode) {
                System.out.printf("      n=%d, L=%d, discrepancy Î”=0x%02X", n, L, d & 0xFF);
            }
            
            if (d == 0) {
                m++;
                if (debugMode) System.out.println(" - no change");
            } else {
                // T(x) = C(x)
                GFPolynomial T = C;
                
                // Compute correction: C(x) = C(x) - (d/b) * x^m * B(x)
                byte scale = field.divide(d, b);
                
                // Create x^m * B(x)
                byte[] shiftedCoeffs = new byte[B.getCoefficients().length + m];
                System.arraycopy(B.getCoefficients(), 0, shiftedCoeffs, m, B.getCoefficients().length);
                GFPolynomial correction = new GFPolynomial(shiftedCoeffs, field).scale(scale);
                
                // Update C(x)
                C = T.add(correction);
                
                if (2 * L <= n) {
                    L = n + 1 - L;
                    B = T;
                    b = d;
                    m = 1;
                    if (debugMode) System.out.printf(" - update L=%d%n", L);
                } else {
                    m++;
                    if (debugMode) System.out.println(" - minor update");
                }
            }
            
            if (debugMode) {
                System.out.println("      Î›(x) = " + C);
            }
        }
        
        if (debugMode) {
            System.out.println("    BM Algorithm END - Î›(x) = " + C);
            System.out.println("    Number of errors: " + L);
        }
        
        return C;
    }
    
    /**
     * Calculate syndromes: S_i = r(Î±^i) for i = 1 to 2t
     */
    public byte[] computeSyndromes(byte[] received) {
        byte[] syndromes = new byte[2 * t];
        
        for (int i = 0; i < 2 * t; i++) {
            byte x = field.power(field.getPrimitiveElement(), i + 1); // Î±^(i+1)
            syndromes[i] = evaluatePolynomial(received, x);
        }
        
        return syndromes;
    }
    
    /**
     * Evaluate polynomial at given point using Horner's method
     */
    private byte evaluatePolynomial(byte[] coefficients, byte x) {
        byte result = 0;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            result = field.multiply(result, x);
            result = field.add(result, coefficients[i]);
        }
        return result;
    }
    
    /**
     * Compute error locator with erasure support
     */
    private GFPolynomial computeErrorLocatorWithErasures(byte[] syndromes, int[] erasurePositions) {
        // Start with erasure locator polynomial
        GFPolynomial erasureLocator = GFPolynomial.buildErasureLocator(erasurePositions, field);
        
        // Modify syndromes for erasures
        GFPolynomial syndromePoly = new GFPolynomial(syndromes, field);
        GFPolynomial modifiedSyndromePoly = syndromePoly.multiply(erasureLocator);
        
        // Take only terms up to x^(2t-1)
        byte[] modifiedSyndromes = Arrays.copyOf(modifiedSyndromePoly.getCoefficients(), 2 * t);
        
        // Compute error locator for remaining errors
        GFPolynomial errorLocator = computeErrorLocator(modifiedSyndromes);
        
        // Combine erasure and error locators
        return errorLocator.multiply(erasureLocator);
    }
    
    /**
     * Find error positions using Chien search
     */
    private int[] findErrorPositions(GFPolynomial errorLocator) {
        if (debugMode) {
            System.out.println("    Chien Search START - Polynomial: " + errorLocator);
        }
        
        int[] roots = errorLocator.findRoots();
        
        if (debugMode) {
            System.out.println("    Chien Search END - Roots found: " + Arrays.toString(roots));
        }
        
        return roots;
    }
    
    /**
     * Validate error positions are within codeword bounds
     */
    private int[] validateErrorPositions(int[] positions) {
        if (positions == null) return null;
        
        java.util.List<Integer> validPositions = new java.util.ArrayList<>();
        for (int pos : positions) {
            if (pos >= 0 && pos < n) {
                validPositions.add(pos);
            } else if (debugMode) {
                System.out.println("    WARNING: Invalid error position: " + pos);
            }
        }
        
        return validPositions.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Compute error magnitudes using Forney algorithm
     */
    private byte[] computeErrorMagnitudes(byte[] syndromes, GFPolynomial errorLocator, int[] errorPositions) {
        if (debugMode) {
            System.out.println("    Forney Algorithm START");
        }
        
        byte[] errorMagnitudes = new byte[errorPositions.length];
        
        // Compute error evaluator polynomial Î©(x) = S(x) * Î›(x) mod x^(2t)
        GFPolynomial syndromePoly = new GFPolynomial(syndromes, field);
        GFPolynomial errorEvaluator = syndromePoly.multiply(errorLocator);
        byte[] omegaCoeffs = errorEvaluator.getCoefficients();
        if (omegaCoeffs.length > 2 * t) {
            omegaCoeffs = Arrays.copyOf(omegaCoeffs, 2 * t);
        }
        GFPolynomial omega = new GFPolynomial(omegaCoeffs, field);
        
        // Compute formal derivative of error locator
        GFPolynomial errorLocatorPrime = errorLocator.formalDerivative();
        
        if (debugMode) {
            System.out.println("    Î©(x) = " + omega);
            System.out.println("    Î›'(x) = " + errorLocatorPrime);
        }
        
        // Compute error magnitudes using Forney algorithm
        for (int i = 0; i < errorPositions.length; i++) {
            byte x_inv = field.power((byte) 2, -errorPositions[i]); // Î±^(-position)
            
            byte omega_x = omega.evaluate(x_inv);
            byte lambda_prime_x = errorLocatorPrime.evaluate(x_inv);
            
            if (debugMode) {
                System.out.printf("    Position %d: Î±^(-%d)=0x%02X, Î©=0x%02X, Î›'=0x%02X%n",
                               errorPositions[i], errorPositions[i], x_inv & 0xFF, 
                               omega_x & 0xFF, lambda_prime_x & 0xFF);
            }
            
            if (lambda_prime_x == 0) {
                throw new ArithmeticException("Formal derivative zero at error position");
            }
            
            byte magnitude = field.divide(omega_x, lambda_prime_x);
            errorMagnitudes[i] = magnitude;
            
            if (debugMode) {
                System.out.printf("    Error magnitude at position %d: 0x%02X%n",
                               errorPositions[i], magnitude & 0xFF);
            }
        }
        
        if (debugMode) {
            System.out.println("    Forney Algorithm END");
        }
        
        return errorMagnitudes;
    }
    
    /**
     * Correct errors in received codeword
     */
    private byte[] correctErrors(byte[] received, int[] errorPositions, byte[] errorMagnitudes) {
        byte[] corrected = received.clone();
        
        for (int i = 0; i < errorPositions.length; i++) {
            int pos = errorPositions[i];
            if (pos < 0 || pos >= n) {
                throw new IllegalArgumentException("Invalid error position: " + pos);
            }
            byte original = corrected[pos];
            corrected[pos] = field.add(corrected[pos], errorMagnitudes[i]);
            
            if (debugMode) {
                System.out.printf("    Correcting position %d: 0x%02X â†’ 0x%02X%n",
                               pos, original & 0xFF, corrected[pos] & 0xFF);
            }
        }
        
        return corrected;
    }
    
    /**
     * Check if all syndromes are zero (no errors)
     */
    private boolean allZero(byte[] array) {
        for (byte b : array) {
            if (b != 0) return false;
        }
        return true;
    }
    
    /**
     * Get decoder statistics
     */
    public DecoderStatistics getStatistics() {
        return new DecoderStatistics(decodeOperations, correctedErrors, decodeFailures);
    }
    
    /**
     * Reset statistics
     */
    public void resetStatistics() {
        decodeOperations = 0;
        correctedErrors = 0;
        decodeFailures = 0;
    }
    
    // Getters
    public GFPolynomial getGeneratorPolynomial() { return generator; }
    public CCSDSGaloisField getField() { return field; }
    public int getN() { return n; }
    public int getK() { return k; }
    public int getT() { return t; }
    
    /**
     * Decoder statistics container
     */
    public static class DecoderStatistics {
        public final long decodeOperations;
        public final long correctedErrors;
        public final long decodeFailures;
        
        public DecoderStatistics(long decodeOperations, long correctedErrors, long decodeFailures) {
            this.decodeOperations = decodeOperations;
            this.correctedErrors = correctedErrors;
            this.decodeFailures = decodeFailures;
        }
        
        @Override
        public String toString() {
            return String.format("Decoder Stats: operations=%d, corrected=%d, failures=%d",
                               decodeOperations, correctedErrors, decodeFailures);
        }
    }
}