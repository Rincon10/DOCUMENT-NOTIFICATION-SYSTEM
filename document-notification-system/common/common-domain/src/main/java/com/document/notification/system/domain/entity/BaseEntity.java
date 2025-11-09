package com.document.notification.system.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
@Getter
@Setter
@EqualsAndHashCode
public abstract class BaseEntity<ID> {
    private ID id;
}