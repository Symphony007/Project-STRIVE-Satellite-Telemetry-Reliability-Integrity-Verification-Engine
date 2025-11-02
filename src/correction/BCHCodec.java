package correction;

/**
 * DEBUG BCH CODEC - With extensive logging
 */
public class BCHCodec {
    
    private static final boolean DEBUG = true;
    
    public BCHCodec() {
        if (DEBUG) System.out.println("DEBUG: BCHCodec initialized");
    }
    
    public byte[] encode(byte[] data) {
        if (DEBUG) {
            System.out.println("DEBUG: BCH ENCODE START");
            System.out.println("DEBUG: Input data: " + bytesToHex(data));
        }
        
        // For now, just pass through to test pipeline
        byte[] result = data.clone();
        
        if (DEBUG) {
            System.out.println("DEBUG: BCH ENCODE COMPLETE");
            System.out.println("DEBUG: Output data: " + bytesToHex(result));
        }
        
        return result;
    }
    
    public byte[] decode(byte[] encoded) {
        if (DEBUG) {
            System.out.println("DEBUG: BCH DECODE START");
            System.out.println("DEBUG: Input encoded: " + bytesToHex(encoded));
        }
        
        // For now, just pass through to test pipeline
        byte[] result = encoded.clone();
        
        if (DEBUG) {
            System.out.println("DEBUG: BCH DECODE COMPLETE");
            System.out.println("DEBUG: Output decoded: " + bytesToHex(result));
        }
        
        return result;
    }
    
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    public static double calculateCorrectionRate(int preErrors, int postErrors) {
        if (preErrors == 0) return 100.0;
        return (preErrors - postErrors) / (double) preErrors * 100;
    }
}