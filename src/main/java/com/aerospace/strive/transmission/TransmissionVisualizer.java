package com.aerospace.strive.transmission;

import java.util.HexFormat;

/**
 * Visualizes satellite telemetry frames in mission-control style display
 * Shows binary, hex, and decoded data side-by-side
 */
public class TransmissionVisualizer {
    
    // ANSI color codes for terminal output
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String BLUE = "\u001B[34m";
    
    /**
     * Displays a full mission-control style transmission view
     */
    public static void displayFrame(TelemetryFrame frame, String status) {
        byte[] binaryData = frame.toBinary();
        
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📡 TRANSMISSION FRAME");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Show hex view (like satellite downlink)
        displayHexView(binaryData);
        
        // Show binary view 
        displayBinaryView(binaryData);
        
        // Show decoded information
        displayDecodedInfo(frame);
        
        // Show status with color coding
        displayStatus(status, frame.getChecksum());
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    /**
     * Shows hexadecimal view of the frame data
     */
    private static void displayHexView(byte[] data) {
        String hex = HexFormat.of().withDelimiter(" ").formatHex(data);
        System.out.println(CYAN + "Hex Frame     : " + hex + RESET);
    }
    
    /**
     * Shows binary view (8-bit grouped)
     */
    private static void displayBinaryView(byte[] data) {
        StringBuilder binary = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 16); i++) { // Show first 16 bytes only
            if (i > 0) binary.append(" ");
            String byteStr = String.format("%8s", Integer.toBinaryString(data[i] & 0xFF))
                                .replace(' ', '0');
            binary.append(byteStr);
        }
        if (data.length > 16) {
            binary.append(" ...");
        }
        System.out.println(BLUE + "Binary Stream : " + binary.toString() + RESET);
    }
    
    /**
     * Shows decoded telemetry information
     */
    private static void displayDecodedInfo(TelemetryFrame frame) {
        System.out.println("Decoded Data  : " + frame.toString());
    }
    
    /**
     * Shows status with color coding
     */
    private static void displayStatus(String status, long checksum) {
        String color = GREEN;
        String icon = "✅";
        
        if (status.contains("CORRUPTED") || status.contains("FAIL")) {
            color = RED;
            icon = "❌";
        } else if (status.contains("SYNC_LOSS")) {
            color = YELLOW;
            icon = "⚠️ ";
        }
        
        System.out.printf("%sCRC: %08X | STATUS: %s %s%s\n", 
                         color, checksum, icon, status, RESET);
    }
    
    /**
     * Simple test method - you can call this from Main to see it work
     */
    public static void demo() {
        System.out.println("🎮 Transmission Visualizer Demo");
        System.out.println("=================================");
        
        // Create a sample frame and display it
        TelemetryFrame frame = FrameBuilder.createSampleFrame();
        displayFrame(frame, "VALID");
        
        // Show what a corrupted frame looks like
        System.out.println(YELLOW + "⚠️  Simulating corrupted transmission..." + RESET);
        displayFrame(frame, "CRC_FAIL");
    }
}