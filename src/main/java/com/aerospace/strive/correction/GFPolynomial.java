package com.aerospace.strive.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NASA-grade polynomial operations in GF(256) for Reed-Solomon coding
 * FIXED VERSION - with enhanced Chien search
 */
public class GFPolynomial {
    private final byte[] coefficients; // coefficients[0] is constant term
    private final CCSDSGaloisField field;
    
    // Performance monitoring
    private final AtomicLong evaluationCount = new AtomicLong(0);
    private final AtomicLong multiplicationCount = new AtomicLong(0);
    private final AtomicLong rootFindingCount = new AtomicLong(0);
    
    // Parallel computation
    private static final ForkJoinPool PARALLEL_POOL = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    
    // Debug mode
    private boolean debugMode = true;
    
    public GFPolynomial(byte[] coefficients, CCSDSGaloisField field) {
        this.field = field;
        this.coefficients = normalize(coefficients);
    }
    
    /**
     * Remove leading zeros from polynomial with mathematical validation
     */
    private byte[] normalize(byte[] coeffs) {
        if (coeffs == null || coeffs.length == 0) {
            return new byte[]{0};
        }
        
        int degree = coeffs.length - 1;
        while (degree >= 0 && coeffs[degree] == 0) {
            degree--;
        }
        
        if (degree < 0) {
            return new byte[]{0}; // Zero polynomial
        }
        
        // Validate all coefficients are proper field elements
        for (int i = 0; i <= degree; i++) {
            if (!field.isValidFieldElement(coeffs[i])) {
                throw new IllegalArgumentException("Invalid field element in coefficient: " + (coeffs[i] & 0xFF));
            }
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
     * Check if polynomial is zero polynomial
     */
    public boolean isZero() {
        return coefficients.length == 1 && coefficients[0] == 0;
    }
    
    /**
     * Get coefficient at given position with bounds checking
     */
    public byte getCoefficient(int index) {
        if (index < 0 || index >= coefficients.length) {
            return 0;
        }
        return coefficients[index];
    }
    
    /**
     * Get all coefficients (defensive copy)
     */
    public byte[] getCoefficients() {
        return coefficients.clone();
    }
    
    /**
     * Evaluate polynomial at point x using Horner's method
     * Optimized for real-time satellite processing
     */
    public byte evaluate(byte x) {
        evaluationCount.incrementAndGet();
        
        if (coefficients.length == 0) return 0;
        if (x == 0) return getCoefficient(0); // Constant term
        
        byte result = coefficients[coefficients.length - 1];
        for (int i = coefficients.length - 2; i >= 0; i--) {
            result = field.multiply(result, x);
            result = field.add(result, coefficients[i]);
        }
        return result;
    }
    
    /**
     * Parallel evaluation at multiple points for real-time performance
     * Critical for syndrome calculation in satellite communications
     */
    public byte[] evaluateParallel(byte[] points) {
        if (points == null || points.length == 0) {
            return new byte[0];
        }
        
        ParallelEvaluationTask task = new ParallelEvaluationTask(points, 0, points.length);
        return PARALLEL_POOL.invoke(task);
    }
    
    // REMOVED DUPLICATE CLASS DEFINITION - KEEP ONLY THIS ONE
    private class ParallelEvaluationTask extends RecursiveTask<byte[]> {
        private final byte[] points;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 64; // Optimize for cache lines
        
        public ParallelEvaluationTask(byte[] points, int start, int end) {
            this.points = points;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected byte[] compute() {
            int length = end - start;
            if (length <= THRESHOLD) {
                // Sequential evaluation for small arrays
                byte[] results = new byte[length];
                for (int i = 0; i < length; i++) {
                    results[i] = evaluate(points[start + i]);
                }
                return results;
            } else {
                // Split and evaluate in parallel
                int mid = start + length / 2;
                ParallelEvaluationTask leftTask = new ParallelEvaluationTask(points, start, mid);
                ParallelEvaluationTask rightTask = new ParallelEvaluationTask(points, mid, end);
                
                leftTask.fork();
                byte[] rightResult = rightTask.compute();
                byte[] leftResult = leftTask.join();
                
                // Combine results
                byte[] combined = new byte[leftResult.length + rightResult.length];
                System.arraycopy(leftResult, 0, combined, 0, leftResult.length);
                System.arraycopy(rightResult, 0, combined, leftResult.length, rightResult.length);
                return combined;
            }
        }
    }
    
    /**
     * Add two polynomials in GF(256) - XOR operation
     */
    public GFPolynomial add(GFPolynomial other) {
        if (other == null) throw new IllegalArgumentException("Other polynomial cannot be null");
        
        int maxLength = Math.max(coefficients.length, other.coefficients.length);
        byte[] result = new byte[maxLength];
        
        for (int i = 0; i < maxLength; i++) {
            byte a = getCoefficient(i);
            byte b = other.getCoefficient(i);
            result[i] = field.add(a, b); // Addition in GF(256) is XOR
        }
        
        return new GFPolynomial(result, field);
    }
    
    /**
     * Multiply two polynomials in GF(256) with performance optimization
     */
    public GFPolynomial multiply(GFPolynomial other) {
        multiplicationCount.incrementAndGet();
        
        if (other == null) throw new IllegalArgumentException("Other polynomial cannot be null");
        if (isZero() || other.isZero()) {
            return new GFPolynomial(new byte[]{0}, field);
        }
        
        byte[] result = new byte[coefficients.length + other.coefficients.length - 1];
        
        for (int i = 0; i < coefficients.length; i++) {
            for (int j = 0; j < other.coefficients.length; j++) {
                byte product = field.multiply(coefficients[i], other.coefficients[j]);
                result[i + j] = field.add(result[i + j], product);
            }
        }
        
        return new GFPolynomial(result, field);
    }
    
    /**
     * Multiply polynomial by scalar in GF(256)
     */
    public GFPolynomial scale(byte scalar) {
        if (scalar == 0) {
            return new GFPolynomial(new byte[]{0}, field);
        }
        if (scalar == 1) {
            return this;
        }
        
        byte[] result = new byte[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            result[i] = field.multiply(coefficients[i], scalar);
        }
        return new GFPolynomial(result, field);
    }
    
    /**
     * Compute formal derivative of polynomial for error correction
     * In GF(256) with characteristic 2:
     * d/dx (a_n x^n) = n·a_n x^{n-1} for odd n, 0 for even n
     */
    public GFPolynomial formalDerivative() {
        if (coefficients.length <= 1) {
            return new GFPolynomial(new byte[]{0}, field);
        }
        
        byte[] derivative = new byte[coefficients.length - 1];
        for (int i = 1; i < coefficients.length; i++) {
            if (i % 2 == 1) { // Only odd powers contribute in characteristic 2
                // Coefficient becomes i * a_i (in GF(256), i mod 256)
                derivative[i - 1] = (byte) (i % 256);
                derivative[i - 1] = field.multiply(derivative[i - 1], coefficients[i]);
            }
        }
        
        return new GFPolynomial(derivative, field);
    }
    
    /**
     * Construct erasure locator polynomial from known erasure positions
     * Γ(x) = Π (1 - α^{position} · x) for each erasure position
     */
    public static GFPolynomial buildErasureLocator(int[] erasurePositions, CCSDSGaloisField field) {
        if (erasurePositions == null || erasurePositions.length == 0) {
            return new GFPolynomial(new byte[]{1}, field);
        }
        
        GFPolynomial locator = new GFPolynomial(new byte[]{1}, field);
        
        for (int position : erasurePositions) {
            if (position < 0 || position >= field.getFieldOrder()) {
                throw new IllegalArgumentException("Invalid erasure position: " + position);
            }
            
            byte alphaPower = field.power(field.getPrimitiveElement(), position);
            // Factor: (α^position + x) which is (1 - α^position · x) in multiplicative form
            byte[] factorCoeffs = new byte[]{alphaPower, 1};
            GFPolynomial factor = new GFPolynomial(factorCoeffs, field);
            locator = locator.multiply(factor);
        }
        
        return locator;
    }
    
    /**
     * Find roots of polynomial using Chien search algorithm - FIXED VERSION
     * Returns positions i where P(α^{-i}) = 0
     * Enhanced with debugging and better search
     */
    public int[] findRoots() {
        rootFindingCount.incrementAndGet();
        
        if (isZero()) {
            throw new IllegalArgumentException("Zero polynomial has infinite roots");
        }
        
        List<Integer> rootPositions = new ArrayList<>();
        
        // CRITICAL FIX: Only search within valid codeword positions (0 to n-1)
        // For RS(15,11), search positions 0-14 only
        int searchLimit = 15; // Only search actual codeword positions
        
        if (debugMode) {
            System.out.println("      Chien Search DEBUG - Polynomial: " + this);
            System.out.println("      Degree: " + degree());
            System.out.println("      Searching positions: 0 to " + (searchLimit-1));
        }
        
        // Evaluate polynomial at α^{-i} for i = 0 to n-1 ONLY
        for (int i = 0; i < searchLimit; i++) {
            byte x = field.power(field.getPrimitiveElement(), -i); // α^{-i}
            byte value = evaluate(x);
            
            if (debugMode) {
                System.out.printf("        i=%d, α^(-%d)=0x%02X, P(α^(-%d))=0x%02X %s%n",
                            i, i, x & 0xFF, i, value & 0xFF,
                            value == 0 ? "← ROOT!" : "");
            }
            
            if (value == 0) {
                rootPositions.add(i);
                if (debugMode) {
                    System.out.printf("      FOUND VALID ROOT at codeword position %d%n", i);
                }
            }
        }
        
        if (debugMode) {
            System.out.println("      Chien Search COMPLETE - Valid Roots: " + rootPositions);
        }
        
        return rootPositions.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Polynomial modulus operation for Reed-Solomon encoding
     * dividend mod divisor in GF(256)
     */
    public GFPolynomial polynomialMod(GFPolynomial divisor) {
        if (divisor == null) throw new IllegalArgumentException("Divisor polynomial cannot be null");
        if (divisor.isZero()) throw new ArithmeticException("Division by zero polynomial");
        
        byte[] remainder = coefficients.clone();
        byte[] divisorCoeffs = divisor.coefficients;
        
        int divisorDegree = divisor.degree();
        if (divisorDegree < 0) {
            throw new ArithmeticException("Division by zero polynomial");
        }
        
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
                        remainder[pos] = field.add(remainder[pos], product); // XOR in GF(2^8)
                    }
                }
            }
        }
        
        // Return remainder (lower degree terms)
        return new GFPolynomial(Arrays.copyOf(remainder, divisorDegree), field);
    }

    
    /**
     * Polynomial division: returns [quotient, remainder]
     */
    public GFPolynomial[] divide(GFPolynomial divisor) {
        if (divisor == null) throw new IllegalArgumentException("Divisor polynomial cannot be null");
        if (divisor.isZero()) throw new ArithmeticException("Division by zero polynomial");
        
        if (degree() < divisor.degree()) {
            return new GFPolynomial[] {
                new GFPolynomial(new byte[]{0}, field), // quotient
                this // remainder
            };
        }
        
        byte[] quotient = new byte[coefficients.length - divisor.degree()];
        byte[] remainder = coefficients.clone();
        byte[] divisorCoeffs = divisor.coefficients;
        
        int divisorDegree = divisor.degree();
        byte divisorLead = divisorCoeffs[divisorDegree];
        
        for (int i = remainder.length - 1; i >= divisorDegree; i--) {
            if (remainder[i] != 0) {
                byte scale = field.divide(remainder[i], divisorLead);
                quotient[i - divisorDegree] = scale;
                
                for (int j = 0; j <= divisorDegree; j++) {
                    int pos = i - divisorDegree + j;
                    byte product = field.multiply(divisorCoeffs[j], scale);
                    remainder[pos] = field.add(remainder[pos], product);
                }
            }
        }
        
        return new GFPolynomial[] {
            new GFPolynomial(quotient, field),
            new GFPolynomial(Arrays.copyOf(remainder, divisorDegree), field)
        };
    }
    
    /**
     * Check if polynomial has specific root
     */
    public boolean hasRoot(byte root) {
        return evaluate(root) == 0;
    }
    
    /**
     * Get performance statistics
     */
    public PolynomialStatistics getStatistics() {
        return new PolynomialStatistics(
            evaluationCount.get(),
            multiplicationCount.get(),
            rootFindingCount.get()
        );
    }
    
    /**
     * Reset performance counters
     */
    public void resetCounters() {
        evaluationCount.set(0);
        multiplicationCount.set(0);
        rootFindingCount.set(0);
    }
    
    @Override
    public String toString() {
        if (isZero()) return "0";
        
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GFPolynomial that = (GFPolynomial) obj;
        return Arrays.equals(coefficients, that.coefficients);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(coefficients);
    }
    
    // Getters
    public CCSDSGaloisField getField() { return field; }
    
    /**
     * Performance statistics container
     */
    public static class PolynomialStatistics {
        public final long evaluations;
        public final long multiplications;
        public final long rootFindings;
        
        public PolynomialStatistics(long evaluations, long multiplications, long rootFindings) {
            this.evaluations = evaluations;
            this.multiplications = multiplications;
            this.rootFindings = rootFindings;
        }
        
        @Override
        public String toString() {
            return String.format("Polynomial Operations: eval=%d, mult=%d, roots=%d",
                               evaluations, multiplications, rootFindings);
        }
    }
}