import correction.ErrorClassifier;
import correction.ErrorPatternAnalyzer;

/**
 * STANDALONE CLASSIFICATION SYSTEM TEST
 */
public class TestClassificationSystem {
    public static void main(String[] args) {
        System.out.println("=== ERROR CLASSIFICATION SYSTEM TEST ===\n");
        
        ErrorClassifier classifier = new ErrorClassifier();
        
        // Test various error scenarios
        testScenario(classifier, "CLEAN_SYNC_DRIFT", 
                    0.10, 0.05, 0.75, 0.02, 0.08, 200, 0.20);
        
        testScenario(classifier, "RANDOM_ERRORS", 
                    0.05, 0.35, 0.10, 0.05, 0.15, 150, 0.15);
        
        testScenario(classifier, "HEAVY_BURSTS", 
                    0.45, 0.10, 0.15, 0.08, 0.12, 300, 0.30);
        
        testScenario(classifier, "MIXED_SYNC_RANDOM", 
                    0.15, 0.25, 0.35, 0.05, 0.10, 180, 0.18);
        
        testScenario(classifier, "MINOR_CORRUPTION", 
                    0.02, 0.03, 0.04, 0.01, 0.02, 5, 0.005);
        
        testScenario(classifier, "GAUSSIAN_NOISE", 
                    0.08, 0.12, 0.10, 0.08, 0.40, 120, 0.12);
        
        // Algorithm information
        System.out.println("\n--- ALGORITHM PERFORMANCE GUIDE ---");
        System.out.println("✅ VITERBI: Use for clear sync drift only");
        System.out.println("✅ BCH: Safe default for random/burst errors");
        System.out.println("🚫 VITERBI: Avoid for random/burst errors (makes worse)");
        System.out.println("🔒 SYSTEM: Conservative routing with safety checks");
    }
    
    private static void testScenario(ErrorClassifier classifier, String scenarioName,
                                   double burst, double random, double sync, 
                                   double loss, double noise, int totalErrors, double density) {
        
        // Determine primary error type
        ErrorPatternAnalyzer.ErrorType primaryType;
        if (sync > 0.3 && sync > burst && sync > random) {
            primaryType = ErrorPatternAnalyzer.ErrorType.SYNC_DRIFT;
        } else if (random > 0.25 && random > burst) {
            primaryType = ErrorPatternAnalyzer.ErrorType.RANDOM_BIT_ERROR;
        } else if (burst > 0.3) {
            primaryType = ErrorPatternAnalyzer.ErrorType.BURST_ERROR;
        } else if (noise > 0.3) {
            primaryType = ErrorPatternAnalyzer.ErrorType.GAUSSIAN_NOISE;
        } else if (density < 0.01) {
            primaryType = ErrorPatternAnalyzer.ErrorType.MINOR_CORRUPTION;
        } else {
            primaryType = ErrorPatternAnalyzer.ErrorType.MIXED_ERRORS;
        }
        
        // Create analysis
        ErrorPatternAnalyzer.ErrorAnalysis analysis = 
            new ErrorPatternAnalyzer.ErrorAnalysis(
                primaryType, burst, random, sync, loss, noise,
                totalErrors, (int)(burst * totalErrors), density
            );
        
        // Get classification
        ErrorClassifier.CorrectionStrategy strategy = 
            classifier.classifyOptimalStrategy(analysis);
        
        // Display results
        System.out.println("📊 " + scenarioName + ":");
        System.out.println("  Type: " + primaryType + ", Errors: " + totalErrors + 
                         ", Density: " + String.format("%.1f%%", density * 100));
        System.out.println("  Scores - Sync:" + String.format("%.2f", sync) + 
                         " Random:" + String.format("%.2f", random) +
                         " Burst:" + String.format("%.2f", burst));
        System.out.println("  " + strategy);
        System.out.println("  Rationale: " + strategy.rationale);
        System.out.println();
    }
}