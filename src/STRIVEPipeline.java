import correction.ViterbiDecoder;
import data.ISSDataFetcher;
import data.TelemetryParser;
import errors.ScientificErrorInjector;
import frames.TelemetryFrame;

/**
 * NASA STRIVE PIPELINE - WITH WORKING VITERBI
 */
public class STRIVEPipeline {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== NASA STRIVE PIPELINE - WORKING VITERBI ===");
            System.out.println("Testing complete error correction pipeline\n");
            
            for (int iteration = 0; iteration < 3; iteration++) {
                System.out.println("=".repeat(60));
                System.out.println("ITERATION " + (iteration + 1));
                
                // SINGLE API CALL
                ISSDataFetcher fetcher = new ISSDataFetcher();
                String rawData = fetcher.fetchLiveTelemetry();
                TelemetryParser parser = new TelemetryParser();
                TelemetryParser.TelemetryData telemetry = parser.parseRawTelemetry(rawData);
                
                System.out.println("ISS: LAT:" + String.format("%.3f", telemetry.latitude) + 
                                 " LON:" + String.format("%.3f", telemetry.longitude));
                
                // BUILD CLEAN FRAME
                TelemetryFrame frameBuilder = new TelemetryFrame();
                byte[] cleanFrame = frameBuilder.buildFromTelemetry(telemetry);
                System.out.println("Clean frame: " + cleanFrame.length + " bytes, CRC: " + frameBuilder.validateFrame());
                
                // TEST 1: VITERBI ONLY
                System.out.println("\n--- VITERBI TEST ---");
                ViterbiDecoder viterbi = new ViterbiDecoder();
                
                // Encode clean data
                byte[] encoded = viterbi.encode(cleanFrame);
                System.out.println("Encoded: " + encoded.length + " bytes (Rate 1/2)");
                
                // Inject errors on ENCODED data
                ScientificErrorInjector errorInjector = new ScientificErrorInjector();
                byte[] corruptedEncoded = errorInjector.injectRealisticErrors(encoded, 0.3);
                
                // Count errors on ENCODED data (proper comparison)
                int encodedErrors = countBitErrors(encoded, corruptedEncoded);
                System.out.println("Errors on encoded data: " + encodedErrors);
                
                // Decode with Viterbi
                byte[] viterbiCorrected = viterbi.decode(corruptedEncoded);
                
                // Count errors on DECODED vs ORIGINAL (proper comparison)
                int viterbiErrors = countBitErrors(cleanFrame, viterbiCorrected);
                System.out.println("Errors after Viterbi: " + viterbiErrors);
                
                // Calculate actual performance
                if (encodedErrors > 0) {
                    double correctionRate = (encodedErrors - viterbiErrors) * 100.0 / encodedErrors;
                    System.out.println("Viterbi correction: " + String.format("%.1f%%", correctionRate));
                }
                
                // Validate frame
                TelemetryFrame viterbiFrame = new TelemetryFrame();
                System.arraycopy(viterbiCorrected, 0, viterbiFrame.getFrameData(), 0, viterbiCorrected.length);
                System.out.println("Frame valid after Viterbi: " + viterbiFrame.validateFrame());
                
                // Wait for next position
                if (iteration < 2) {
                    System.out.println("Waiting 6 seconds...");
                    Thread.sleep(6000);
                }
            }
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PIPELINE TEST COMPLETE");
            
        } catch (Exception e) {
            System.err.println("Pipeline failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static int countBitErrors(byte[] a, byte[] b) {
        int errors = 0;
        int minLength = Math.min(a.length, b.length);
        for (int i = 0; i < minLength; i++) {
            errors += Integer.bitCount(a[i] ^ b[i]);
        }
        return errors;
    }
}