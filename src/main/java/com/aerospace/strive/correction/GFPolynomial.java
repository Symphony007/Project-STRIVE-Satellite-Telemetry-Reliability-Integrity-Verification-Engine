package com.aerospace.strive.correction;

import java.util.Arrays;

/**
 * Polynomial operations in GF(256) for Reed-Solomon coding
 * Represents polynomials with coefficients in CCSDS Galois Field
 */
public class GFPolynomial {
    private final byte[] coefficients; // coefficients[0] is constant term
    private final CCSDSGaloisField field;
    
    public GFPolynomial(byte[] coefficients, CCSDSGaloisField field) {
        this.field = field;
        this.coefficients = normalize(coefficients);
    }
    
    /**
     * Remove leading zeros from polynomial
     */
    private byte[] normalize(byte[] coeffs) {
        int degree = coeffs.length - 1;
        while (degree >= 0 && coeffs[degree] == 0) {
            degree--;
        }
        
        if (degree < 0) {
            return new byte[]{0}; // Zero polynomial
        }
        
        return Arrays.copyOf(coeffs, degree + 1);
    }
    
    /**
     * Get polynomial degree (highest non-zero coefficient)
     */
    public int degree() {
        return coefficients.length - 1;
    }
    
    /**
     * Get coefficient at given position
     */
    public byte getCoefficient(int index) {
        if (index < 0 || index >= coefficients.length) {
            return 0;
        }
        return coefficients[index];
    }
    
    /**
     * Evaluate polynomial at point x using Horner's method
     */
    public byte evaluate(byte x) {
        if (coefficients.length == 0) return 0;
        
        byte result = coefficients[coefficients.length - 1];
        for (int i = coefficients.length - 2; i >= 0; i--) {
            result = field.multiply(result, x);
            result = (byte) (result ^ coefficients[i]);
        }
        return result;
    }
    
    /**
     * Add two polynomials in GF(256)
     */
    public GFPolynomial add(GFPolynomial other) {
        int maxLength = Math.max(coefficients.length, other.coefficients.length);
        byte[] result = new byte[maxLength];
        
        for (int i = 0; i < maxLength; i++) {
            byte a = getCoefficient(i);
            byte b = other.getCoefficient(i);
            result[i] = (byte) (a ^ b); // Addition in GF(256) is XOR
        }
        
        return new GFPolynomial(result, field);
    }
    
    /**
     * Multiply two polynomials in GF(256)
     */
    public GFPolynomial multiply(GFPolynomial other) {
        if (coefficients.length == 0 || other.coefficients.length == 0) {
            return new GFPolynomial(new byte[]{0}, field);
        }
        
        byte[] result = new byte[coefficients.length + other.coefficients.length - 1];
        
        for (int i = 0; i < coefficients.length; i++) {
            for (int j = 0; j < other.coefficients.length; j++) {
                byte product = field.multiply(coefficients[i], other.coefficients[j]);
                result[i + j] = (byte) (result[i + j] ^ product);
            }
        }
        
        return new GFPolynomial(result, field);
    }
    
    /**
     * Multiply polynomial by scalar in GF(256)
     */
    public GFPolynomial scale(byte scalar) {
        byte[] result = new byte[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            result[i] = field.multiply(coefficients[i], scalar);
        }
        return new GFPolynomial(result, field);
    }
    
    /**
     * Compute formal derivative of polynomial
     * In GF(256), derivative drops even powers and odd powers become (power * coefficient)
     */
    public GFPolynomial formalDerivative() {
        if (coefficients.length <= 1) {
            return new GFPolynomial(new byte[]{0}, field);
        }
        
        byte[] derivative = new byte[coefficients.length - 1];
        for (int i = 1; i < coefficients.length; i++) {
            if (i % 2 == 1) { // Odd powers only (in GF characteristic 2)
                derivative[i - 1] = (byte) (i % 256); // In GF(256), i mod 256
                derivative[i - 1] = field.multiply(derivative[i - 1], coefficients[i]);
            }
        }
        
        return new GFPolynomial(derivative, field);
    }
    
    @Override
    public String toString() {
        if (coefficients.length == 0) return "0";
        
        StringBuilder sb = new StringBuilder();
        for (int i = coefficients.length - 1; i >= 0; i--) {
            if (coefficients[i] != 0) {
                if (sb.length() > 0) sb.append(" + ");
                if (i == 0) {
                    sb.append(String.format("0x%02X", coefficients[i]));
                } else if (i == 1) {
                    sb.append(String.format("0x%02X·x", coefficients[i]));
                } else {
                    sb.append(String.format("0x%02X·x^%d", coefficients[i], i));
                }
            }
        }
        return sb.toString();
    }
    
    // Getters
    public byte[] getCoefficients() { return coefficients.clone(); }
    public CCSDSGaloisField getField() { return field; }
}