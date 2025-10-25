package com.aerospace.strive.producer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class ISSDataProducer {
    
    private static final String KAFKA_BROKER = "localhost:9092";
    private static final String TOPIC_NAME = "iss-telemetry";
    private static final String ISS_API_URL = "https://api.wheretheiss.at/v1/satellites/25544";
    
    public static void main(String[] args) {
        System.out.println("🛰️ Starting Real ISS Telemetry Producer...");
        
        // Configure Kafka Producer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            
            HttpClient client = HttpClient.newHttpClient();
            
            // Fetch and send 5 real ISS data points
            for (int i = 1; i <= 5; i++) {
                try {
                    // Fetch real ISS data
                    String issData = fetchISSData(client);
                    System.out.println("📡 Fetched real ISS data: " + issData);
                    
                    // Create Kafka record
                    ProducerRecord<String, String> record = 
                        new ProducerRecord<>(TOPIC_NAME, "iss", issData);
                    
                    // Send to Kafka
                    RecordMetadata metadata = producer.send(record).get();
                    
                    System.out.printf("✅ Sent ISS packet %d to partition %d, offset %d%n",
                        i, metadata.partition(), metadata.offset());
                    
                    // Wait 3 seconds between fetches (respect API rate limits)
                    Thread.sleep(3000);
                    
                } catch (Exception e) {
                    System.err.println("❌ Error fetching ISS data: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Successfully sent 5 real ISS telemetry packets!");
            
        } catch (Exception e) {
            System.err.println("❌ Error in producer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String fetchISSData(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ISS_API_URL))
                .GET()
                .build();
                
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}