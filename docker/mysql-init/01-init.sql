-- Project STRIVE Database Initialization
CREATE DATABASE IF NOT EXISTS telemetry_db;
USE telemetry_db;

-- Table for raw telemetry packets
CREATE TABLE IF NOT EXISTS raw_packets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    packet_id VARCHAR(100) NOT NULL,
    timestamp BIGINT NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    raw_data BLOB NOT NULL,
    checksum BIGINT NOT NULL,
    is_corrupted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_timestamp (timestamp),
    INDEX idx_sensor_id (sensor_id)
);

-- Table for corrected packets
CREATE TABLE IF NOT EXISTS corrected_packets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_packet_id BIGINT NOT NULL,
    corrected_data BLOB NOT NULL,
    correction_method ENUM('REED_SOLOMON', 'PARITY', 'NONE') NOT NULL,
    correction_success BOOLEAN NOT NULL,
    processing_time_ms INT,
    corrected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (original_packet_id) REFERENCES raw_packets(id),
    INDEX idx_corrected_at (corrected_at)
);

-- Table for error logs and statistics
CREATE TABLE IF NOT EXISTS error_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    packet_id BIGINT,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (packet_id) REFERENCES raw_packets(id),
    INDEX idx_created_at (created_at),
    INDEX idx_severity (severity)
);

-- Table for system metrics
CREATE TABLE IF NOT EXISTS system_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    packets_processed INT DEFAULT 0,
    packets_corrupted INT DEFAULT 0,
    packets_corrected INT DEFAULT 0,
    error_rate DECIMAL(5,4) DEFAULT 0.0,
    correction_success_rate DECIMAL(5,4) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_timestamp (timestamp)
);

-- Create user and grant permissions (if they don't exist)
CREATE USER IF NOT EXISTS 'strive_user'@'%' IDENTIFIED BY 'strive123';
GRANT ALL PRIVILEGES ON telemetry_db.* TO 'strive_user'@'%';
FLUSH PRIVILEGES;