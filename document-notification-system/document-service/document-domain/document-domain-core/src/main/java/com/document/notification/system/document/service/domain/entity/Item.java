package com.document.notification.system.document.service.domain.entity;

import com.document.notification.system.domain.valueobject.Money;
import lombok.Builder;
import lombok.Data;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Data
@Builder
public class Item {
    private String name;
    private Money amount;
}
