package com.aerospace.strive.correction;

/**
 * Test GFPolynomial operations
 */
public class TestGFPolynomial {
    public static void main(String[] args) {
        System.out.println("🧪 Testing GFPolynomial Operations");
        System.out.println("==================================");
        
        CCSDSGaloisField gf = new CCSDSGaloisField();
        
        // Test polynomial: 3x^2 + 2x + 1
        byte[] coeffs1 = {0x01, 0x02, 0x03}; // 1 + 2x + 3x²
        GFPolynomial poly1 = new GFPolynomial(coeffs1, gf);
        
        // Test polynomial: x + 1
        byte[] coeffs2 = {0x01, 0x01}; // 1 + x
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
        byte x = 0x02; // Evaluate at x = 2
        byte result = poly1.evaluate(x);
        System.out.printf("Evaluation at x=0x%02X: 0x%02X%n", x, result);
        
        // Test formal derivative
        GFPolynomial derivative = poly1.formalDerivative();
        System.out.println("Formal Derivative: " + derivative);
        
        // Test scaling
        GFPolynomial scaled = poly1.scale((byte)0x04);
        System.out.println("Scaled by 0x04: " + scaled);
        
        System.out.println("✅ GFPolynomial test completed!");
    }
}