package com.document.notification.system.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@AllArgsConstructor
@Getter
public class Document {
    public static final String FAILURE_MESSAGE_DELIMITER = ",";
    private List<String> failureMessages;
}
