package com.document.notification.system.kafka.consumer;

import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
public interface KafkaConsumer<T extends SpecificRecordBase> {
    void receive(List<T> messages, List<String> keys, List<Integer> partitions, List<Long> offsets);
}
