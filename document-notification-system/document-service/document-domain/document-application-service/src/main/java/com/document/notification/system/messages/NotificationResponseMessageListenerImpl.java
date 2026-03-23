package com.document.notification.system.messages;

import com.document.notification.system.dto.message.NotificationResponse;
import com.document.notification.system.ports.input.message.listener.notification.NotificationResponseMessageListener;
import com.document.notification.system.saga.DocumentNotificationSaga;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */

@Slf4j
@Service
@Validated
@AllArgsConstructor
public class NotificationResponseMessageListenerImpl implements NotificationResponseMessageListener {
    private final DocumentNotificationSaga documentNotificationSaga;

    @Override
    public void notificationCompleted(NotificationResponse notificationResponse) {
        documentNotificationSaga.execute(notificationResponse);
        log.info("Document Notification Saga process operation is completed for document id: {}", notificationResponse.getDocumentId());
    }

    @Override
    public void notificationFailed(NotificationResponse notificationResponse) {
        documentNotificationSaga.compensate(notificationResponse);
        log.info("Document Notification Saga compensation is triggered for document id: {}", notificationResponse.getDocumentId());
    }
}
