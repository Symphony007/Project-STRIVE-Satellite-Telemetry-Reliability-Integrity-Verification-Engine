package com.aerospace.strive.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ValidationConsumer {
    
    private static final String KAFKA_BROKER = "localhost:9092";
    private static final String TOPIC_NAME = "telemetry-raw";
    private static final String GROUP_ID = "validation-consumer-group";
    
    public static void main(String[] args) {
        System.out.println("🔍 Starting Satellite Telemetry Validation Consumer...");
        
        // Configure Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            
            consumer.subscribe(Collections.singletonList(TOPIC_NAME));
            System.out.println("✅ Subscribed to topic: " + TOPIC_NAME);
            
            int totalPackets = 0;
            int validPackets = 0;
            int corruptedPackets = 0;
            
            System.out.println("🕐 Listening for messages (10 seconds)...");
            System.out.println("=============================================");
            
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 10000) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                if (records.isEmpty()) {
                    System.out.println("⏳ No messages in this poll...");
                    continue;
                }
                
                records.forEach(record -> {
                    // For simulation: randomly validate or flag as corrupted
                    boolean isValid = simulatePacketValidation(record.value());
                    
                    if (isValid) {
                        System.out.printf("✅ VALID - Partition: %d, Offset: %d, Sensor: %s%n",
                            record.partition(), record.offset(), record.key());
                    } else {
                        System.out.printf("❌ CORRUPTED - Partition: %d, Offset: %d, Sensor: %s%n", 
                            record.partition(), record.offset(), record.key());
                    }
                });
                
                totalPackets += records.count();
                // Simple simulation: 80% valid, 20% corrupted
                validPackets += (int)(records.count() * 0.8);
                corruptedPackets += (int)(records.count() * 0.2);
            }
            
            // Print summary statistics
            System.out.println("=============================================");
            System.out.println("📊 VALIDATION SUMMARY:");
            System.out.printf("📦 Total Packets Processed: %d%n", totalPackets);
            System.out.printf("✅ Valid Packets: %d (%.1f%%)%n", validPackets, 
                totalPackets > 0 ? (validPackets * 100.0 / totalPackets) : 0);
            System.out.printf("❌ Corrupted Packets: %d (%.1f%%)%n", corruptedPackets,
                totalPackets > 0 ? (corruptedPackets * 100.0 / totalPackets) : 0);
            System.out.println("✅ Validation consumer finished!");
            
        } catch (Exception e) {
            System.err.println("❌ Error in validation consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simulate packet validation - in real system, this would use actual checksums
     * For now, we simulate 80% valid, 20% corrupted to demonstrate the concept
     */
    private static boolean simulatePacketValidation(String packetData) {
        return Math.random() > 0.2; // 80% valid, 20% corrupted
    }
}