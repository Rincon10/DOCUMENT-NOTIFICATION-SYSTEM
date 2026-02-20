package com.document.notification.system.outbox.scheduler.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 20/02/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class GeneratorOutboxHelper {

    private final ObjectMapper objectMapper;
}
