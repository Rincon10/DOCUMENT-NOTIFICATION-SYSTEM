package com.document.notification.system.kafka.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-config")
public class KafkaConfigData {
    private String bootstrapServers;
    private String schemaRegistryUrlKey;
    private String schemaRegistryUrl;
    private Integer numOfPartitions;
    private Short replicationFactor;
    // Optional security settings for managed Kafka (Confluent Cloud, AWS MSK, Azure Event Hubs).
    // Left empty for local PLAINTEXT brokers.
    private String securityProtocol;
    private String saslMechanism;
    private String saslJaasConfig;
    private String schemaRegistryBasicAuthCredentialsSource;
    private String schemaRegistryBasicAuthUserInfo;

    public boolean hasSecurityConfig() {
        return securityProtocol != null && !securityProtocol.isBlank();
    }

    public boolean hasSchemaRegistryAuth() {
        return schemaRegistryBasicAuthUserInfo != null && !schemaRegistryBasicAuthUserInfo.isBlank();
    }
}

