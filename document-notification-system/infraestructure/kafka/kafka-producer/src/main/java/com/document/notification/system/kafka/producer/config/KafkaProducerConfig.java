package com.document.notification.system.kafka.producer.config;

import com.document.notification.system.kafka.config.data.KafkaConfigData;
import com.document.notification.system.kafka.config.data.KafkaProducerConfigData;
import lombok.AllArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Configuration
@AllArgsConstructor
public class KafkaProducerConfig<K extends Serializable, V extends SpecificRecordBase> {
    private final KafkaProducerConfigData kafkaProducerConfigData;
    private final KafkaConfigData kafkaConfigData;

    @Bean
    public Map<String, Object> producerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigData.getBootstrapServers());
        props.put(kafkaConfigData.getSchemaRegistryUrlKey(), kafkaConfigData.getSchemaRegistryUrl());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getKeySerializerClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getValueSerializerClass());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProducerConfigData.getBatchSize() *
                kafkaProducerConfigData.getBatchSizeBoostFactor());
        props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProducerConfigData.getLingerMs());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProducerConfigData.getCompressionType());
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProducerConfigData.getAcks());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaProducerConfigData.getRequestTimeoutMs());
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerConfigData.getRetryCount());
        addSecurityProps(props);
        return props;
    }

    private void addSecurityProps(Map<String, Object> props) {
        if (kafkaConfigData.hasSecurityConfig()) {
            props.put("security.protocol", kafkaConfigData.getSecurityProtocol());
            if (kafkaConfigData.getSaslMechanism() != null && !kafkaConfigData.getSaslMechanism().isBlank()) {
                props.put("sasl.mechanism", kafkaConfigData.getSaslMechanism());
            }
            if (kafkaConfigData.getSaslJaasConfig() != null && !kafkaConfigData.getSaslJaasConfig().isBlank()) {
                props.put("sasl.jaas.config", kafkaConfigData.getSaslJaasConfig());
            }
        }
        if (kafkaConfigData.hasSchemaRegistryAuth()) {
            props.put("basic.auth.credentials.source", kafkaConfigData.getSchemaRegistryBasicAuthCredentialsSource());
            props.put("basic.auth.user.info", kafkaConfigData.getSchemaRegistryBasicAuthUserInfo());
        }
    }

    @Bean
    public ProducerFactory<K, V> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    @Bean
    public KafkaTemplate<K, V> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
