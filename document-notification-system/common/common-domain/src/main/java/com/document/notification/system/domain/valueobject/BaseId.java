package com.document.notification.system.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public abstract class BaseId<T> {
    private final T value;
}