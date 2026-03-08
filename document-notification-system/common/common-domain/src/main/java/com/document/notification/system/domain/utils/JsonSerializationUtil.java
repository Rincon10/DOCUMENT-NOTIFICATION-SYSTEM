package com.document.notification.system.domain.utils;

import com.document.notification.system.domain.exceptions.DomainException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonSerializationUtil {
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates and configures an ObjectMapper with Java 8 date/time support.
     *
     * @return configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();


        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /**
     * Converts an object to a JSON string.
     *
     * @param object the object to serialize
     * @param <T>    the generic type of the object
     * @return the JSON string representation of the object
     * @throws DomainException if serialization fails
     */
    public static <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize object to JSON: {}", object.getClass().getName(), e);
            throw new DomainException(
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
     * @throws DomainException if serialization fails
     */
    public static <T> String toJson(T object, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize object to JSON: {}", errorMessage, e);
            throw new DomainException(errorMessage, e);
        }
    }

    /**
     * Converts a JSON string to an object.
     *
     * @param jsonString the JSON string to deserialize
     * @param valueType  the class type to deserialize to
     * @param <T>        the generic type of the object
     * @return the deserialized object
     * @throws DomainException if deserialization fails
     */
    public static <T> T fromJson(String jsonString, Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON to {}: {}", valueType.getSimpleName(), jsonString, e);
            throw new DomainException(
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
     * @throws DomainException if deserialization fails
     */
    public static <T> T fromJson(String jsonString, Class<T> valueType, String errorMessage) {
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize JSON to {}: {}", errorMessage, jsonString, e);
            throw new DomainException(errorMessage, e);
        }
    }
}
