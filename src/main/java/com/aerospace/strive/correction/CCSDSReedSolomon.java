package com.aerospace.strive.correction;

import java.util.Arrays;

/**
 * CCSDS-compliant Reed-Solomon encoder for satellite communications
 * Uses RS(255,223) code with shortening capability for telemetry frames
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
    
    public CCSDSReedSolomon() {
        this(CCSDS_N, CCSDS_K);
    }
    
    public CCSDSReedSolomon(int n, int k) {
        this.field = new CCSDSGaloisField();
        this.n = n;
        this.k = k;
        this.t = (n - k) / 2;
        this.generator = buildGeneratorPolynomial();
        
        System.out.printf("🛰️  CCSDS Reed-Solomon RS(%d,%d) initialized - can correct %d symbol errors%n", 
                         n, k, t);
    }
    
    /**
     * Build generator polynomial: g(x) = (x - α)(x - α²)...(x - α^(2t))
     */
    private GFPolynomial buildGeneratorPolynomial() {
        // Start with g(x) = 1
        GFPolynomial gen = new GFPolynomial(new byte[]{1}, field);
        
        // Multiply by (x - α^i) for i = 1 to 2t
        for (int i = 1; i <= 2 * t; i++) {
            byte alphaPower = field.power((byte) 2, i); // α^i
            GFPolynomial factor = new GFPolynomial(new byte[]{alphaPower, 1}, field);
            gen = gen.multiply(factor);
        }
        
        return gen;
    }
    
    /**
     * Encode message with systematic Reed-Solomon coding
     * Output: [message][parity] 
     */
    public byte[] encode(byte[] message) {
        if (message.length > k) {
            throw new IllegalArgumentException(
                String.format("Message too long: %d > %d", message.length, k));
        }
        
        // For shortened codes, pad message with zeros
        byte[] paddedMessage = Arrays.copyOf(message, k);
        
        // Systematic encoding: message * x^(n-k) mod g(x)
        GFPolynomial messagePoly = new GFPolynomial(paddedMessage, field);
        GFPolynomial shifted = messagePoly.multiply(
            new GFPolynomial(new byte[2 * t], field)); // Multiply by x^(2t)
        
        // Calculate remainder: shifted mod generator
        GFPolynomial remainder = polynomialMod(shifted, generator);
        
        // Systematic codeword: [message][parity]
        byte[] codeword = Arrays.copyOf(paddedMessage, n);
        byte[] remainderCoeffs = remainder.getCoefficients();
        
        // Copy remainder (parity) to end of codeword
        for (int i = 0; i < remainderCoeffs.length; i++) {
            codeword[k + i] = remainderCoeffs[i];
        }
        
        return codeword;
    }
    
    /**
     * Polynomial modulus operation in GF(256)
     */
    private GFPolynomial polynomialMod(GFPolynomial dividend, GFPolynomial divisor) {
        byte[] remainder = dividend.getCoefficients();
        byte[] divisorCoeffs = divisor.getCoefficients();
        
        int divisorDegree = divisor.degree();
        byte divisorLead = divisorCoeffs[divisorDegree];
        
        for (int i = remainder.length - 1; i >= divisorDegree; i--) {
            if (remainder[i] != 0) {
                byte scale = field.divide(remainder[i], divisorLead);
                
                for (int j = 0; j <= divisorDegree; j++) {
                    int pos = i - divisorDegree + j;
                    byte product = field.multiply(divisorCoeffs[j], scale);
                    remainder[pos] = (byte) (remainder[pos] ^ product);
                }
            }
        }
        
        // Return remainder (lower degree terms)
        return new GFPolynomial(Arrays.copyOf(remainder, divisorDegree), field);
    }
    
    /**
     * Get generator polynomial for verification
     */
    public GFPolynomial getGeneratorPolynomial() {
        return generator;
    }
    
    public int getN() { return n; }
    public int getK() { return k; }
    public int getT() { return t; }
}