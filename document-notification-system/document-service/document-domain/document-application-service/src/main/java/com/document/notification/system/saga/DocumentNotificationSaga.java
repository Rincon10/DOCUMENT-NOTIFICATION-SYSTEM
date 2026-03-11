package com.document.notification.system.saga;

import com.document.notification.system.document.service.domain.service.IDocumentDomainService;
import com.document.notification.system.dto.message.NotificationResponse;
import com.document.notification.system.mapper.IDocumentDataMapper;
import com.document.notification.system.outbox.scheduler.generator.GeneratorOutboxHelper;
import com.document.notification.system.outbox.scheduler.notification.NotificationOutboxHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/03/2026
 */
@Slf4j
@Component
@AllArgsConstructor
public class DocumentNotificationSaga implements SagaStep<NotificationResponse> {
    private final IDocumentDataMapper documentDataMapper;
    private final IDocumentDomainService documentDomainService;
    private final IDocumentSagaHelper documentSagaHelper;
    private final NotificationOutboxHelper notificationOutboxHelper;
    private final GeneratorOutboxHelper generatorOutboxHelper;

    @Override
    public void execute(NotificationResponse data) {

    }

    @Override
    public void compensate(NotificationResponse data) {

    }
}
