package com.document.notification.system.domain.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Utility class for safe operations and mapping transformations.
 * Provides methods to execute code safely without throwing exceptions.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 28/02/2026
 */
@Slf4j
public class MapperUtils {

    private MapperUtils() {
        // Private constructor to prevent instantiation
    }


    /**
     * Executes a supplier safely with a default fallback value.
     * If an exception occurs, returns the default value instead of throwing.
     *
     * @param supplier     the supplier function to execute
     * @param defaultValue the fallback value to return if an exception occurs
     * @param <T>          the type of the result
     * @return the supplier result or the default value if an exception occurs
     * <p>
     * Example:
     * <pre>
     * String result = safeOrDefault(() -> riskyOperation(), "default value");
     * System.out.println(result);
     * </pre>
     */
    public static <T> T safeOrDefault(Supplier<T> supplier, T defaultValue) {
        try {
            T result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            log.debug("Exception occurred during safe execution, returning default value: {}", e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Executes a supplier safely with a default fallback value and custom error logging.
     *
     * @param supplier     the supplier function to execute
     * @param defaultValue the fallback value to return if an exception occurs
     * @param errorMessage custom error message to log
     * @param <T>          the type of the result
     * @return the supplier result or the default value if an exception occurs
     * <p>
     * Example:
     * <pre>
     * String result = safeOrDefault(
     *     () -> riskyOperation(),
     *     "default value",
     *     "Operation failed, using default"
     * );
     * </pre>
     */
    public static <T> T safeOrDefault(Supplier<T> supplier, T defaultValue, String errorMessage) {
        try {
            T result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            log.debug("{}: {}", errorMessage, e.getMessage());
            return defaultValue;
        }
    }
}
