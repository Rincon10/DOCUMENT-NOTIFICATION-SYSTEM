package com.document.notification.system.helper;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JSON serialization and deserialization operations.
 * Provides generic methods to convert objects to JSON strings with proper error handling.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Slf4j
public class JsonSerializationUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts an object to a JSON string.
     *
     * @param object the object to serialize
     * @param <T>    the generic type of the object
     * @return the JSON string representation of the object
     * @throws DocumentDomainException if serialization fails
     */
    public static <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize object to JSON: {}", object.getClass().getName(), e);
            throw new DocumentDomainException(
                    String.format("Could not serialize %s to JSON", object.getClass().getSimpleName()),
                    e
            );
        }
    }

    /**
     * Converts an object to a JSON string with a custom error message.
     *
     * @param object       the object to serialize
     * @param errorMessage the custom error message to use if serialization fails
     * @param <T>          the generic type of the object
     * @return the JSON string representation of the object
     * @throws DocumentDomainException if serialization fails
     */
    public static <T> String toJson(T object, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize object to JSON: {}", errorMessage, e);
            throw new DocumentDomainException(errorMessage, e);
        }
    }

    /**
     * Converts a JSON string to an object.
     *
     * @param jsonString the JSON string to deserialize
     * @param valueType  the class type to deserialize to
     * @param <T>        the generic type of the object
     * @return the deserialized object
     * @throws DocumentDomainException if deserialization fails
     */
    public static <T> T fromJson(String jsonString, Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON to {}: {}", valueType.getSimpleName(), jsonString, e);
            throw new DocumentDomainException(
                    String.format("Could not deserialize JSON to %s", valueType.getSimpleName()),
                    e
            );
        }
    }

    /**
     * Converts a JSON string to an object with a custom error message.
     *
     * @param jsonString   the JSON string to deserialize
     * @param valueType    the class type to deserialize to
     * @param errorMessage the custom error message to use if deserialization fails
     * @param <T>          the generic type of the object
     * @return the deserialized object
     * @throws DocumentDomainException if deserialization fails
     */
    public static <T> T fromJson(String jsonString, Class<T> valueType, String errorMessage) {
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON to {}: {}", errorMessage, jsonString, e);
            throw new DocumentDomainException(errorMessage, e);
        }
    }
}

