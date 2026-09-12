package com.document.notification.system.notification.service.domain.helper;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.notification.service.domain.dto.NotificationRequest;
import com.document.notification.system.notification.service.domain.entity.DocumentNotification;
import com.document.notification.system.notification.service.domain.event.NotificationEvent;
import com.document.notification.system.notification.service.domain.mapper.NotificationDataMapper;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.outbox.scheduler.DocumentOutboxHelper;
import com.document.notification.system.notification.service.domain.ports.output.message.publisher.NotificationResponseMessagePublisher;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentNotificationRepository;
import com.document.notification.system.notification.service.domain.service.INotificationDomainService;
import com.document.notification.system.notification.service.domain.valueobject.NotificationData;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class NotificationRequestHelperImpl implements NotificationRequestHelper {
    private final INotificationDomainService notificationDomainService;
    private final NotificationDataMapper notificationDataMapper;
    private final DocumentNotificationRepository documentNotificationRepository;
    private final DocumentOutboxHelper documentOutboxHelper;
    private final NotificationResponseMessagePublisher notificationResponseMessagePublisher;

    /**
     * Deliberadamente SIN {@code @Transactional}: el envio del correo no es transaccional y no debe quedar
     * dentro de una transaccion de BD. El orden es "reservar, actuar, confirmar":
     * <ol>
     *   <li>claim: fila de outbox en PENDING con el indice unico de la saga (transaccion propia, commit ya)</li>
     *   <li>envio del correo, fuera de transaccion</li>
     *   <li>complete: la misma fila pasa a SENT/FAILED y se guarda el historial</li>
     * </ol>
     * Las guardas que solo miran filas escritas DESPUES del envio no pueden evitar un correo duplicado.
     */
    @Override
    public void persistNotificationOnHistoryRecords(NotificationRequest notificationRequest) {
        if (publishIfOutboxMessageProcessedForNotification(notificationRequest, NotificationStatus.NOTIFICATION_SENT)) {
            log.info("An outbox message with saga id: {} is already saved to database!",
                    notificationRequest.getSagaId());
            return;
        }

        UUID sagaId = UUID.fromString(notificationRequest.getSagaId());

        // Redelivery de una saga ya reclamada (PENDING = en curso) o ya terminada (SENT/FAILED): descartar.
        if (documentOutboxHelper.existsDocumentOutboxMessage(sagaId)) {
            log.info("Notification for saga id: {} is already claimed or processed, skipping duplicate delivery.",
                    sagaId);
            return;
        }

        log.info("Received notification event for document id: {}", notificationRequest.getDocumentId());

        DocumentNotification documentNotification = notificationDataMapper
                .notificationRequestToDocumentNotification(notificationRequest);
        documentNotification.initializeNotification();

        // 1. Claim ANTES de enviar. Si dos hilos/replicas llegan a la vez con la misma saga, el indice unico del
        //    outbox decide quien envia; el que pierde recibe la violacion de unicidad y sale sin tocar el correo.
        try {
            documentOutboxHelper.claimDocumentOutboxMessage(pendingPayload(documentNotification), sagaId);
        } catch (DataIntegrityViolationException e) {
            log.info("Notification for saga id: {} was claimed concurrently by another consumer, skipping duplicate delivery.",
                    sagaId);
            return;
        }

        // 2. Envio, fuera de transaccion (no retiene conexion del pool mientras espera rate limiter / SMTP).
        ArrayList<String> failureMessages = new ArrayList<>();
        NotificationData notificationData = getNotificationData(notificationRequest);

        NotificationEvent notificationEvent = notificationDomainService
                .validateAndSendNotification(documentNotification, failureMessages, notificationData);

        // 3. Complete: la fila PENDING pasa al estado final y el scheduler la publica; luego el historial.
        DocumentEventPayload documentEventPayload = notificationDataMapper
                .notificationEventToDocumentEventPayload(notificationEvent);
        documentOutboxHelper.completeDocumentOutboxMessage(documentEventPayload,
                notificationEvent.getDocumentNotification().getNotificationStatus(), sagaId);

        documentNotificationRepository.save(documentNotification);
        log.info("Document notification saved with id: {}", documentNotification.getId().getValue());

        log.info("Notification processing completed for document id: {}", notificationRequest.getDocumentId());
    }

    private DocumentEventPayload pendingPayload(DocumentNotification documentNotification) {
        return DocumentEventPayload.builder()
                .notificationId(documentNotification.getId().getValue().toString())
                .customerId(documentNotification.getCustomerId().getValue().toString())
                .documentId(documentNotification.getDocumentId().getValue().toString())
                .recipientId(documentNotification.getRecipient().getTarget())
                .createdAt(DateUtils.getZoneDateTimeByUTCZoneId())
                .notificationStatus(NotificationStatus.NOTIFICATION_PENDING.name())
                .failureMessages(List.of())
                .build();
    }

    private NotificationData getNotificationData(NotificationRequest notificationRequest) {
        return NotificationData.builder()
                .documentId(StringUtils.trimToNull(notificationRequest.getDocumentId()))
                .customerId(StringUtils.trimToNull(notificationRequest.getCustomerId()))
                .requestId(StringUtils.trimToNull(notificationRequest.getId()))
                .sagaId(StringUtils.trimToNull(notificationRequest.getSagaId()))
                .build();
    }

    private boolean publishIfOutboxMessageProcessedForNotification(NotificationRequest notificationRequest,
                                                                    NotificationStatus notificationStatus) {
        Optional<DocumentOutboxMessage> documentOutboxMessagesOptional = documentOutboxHelper
                .getCompletedDocumentOutboxMessageBySagaIdAndNotificationStatus(
                        UUID.fromString(notificationRequest.getSagaId()),
                        notificationStatus
                );

        if (documentOutboxMessagesOptional.isPresent()) {
            DocumentOutboxMessage documentOutboxMessage = documentOutboxMessagesOptional.get();
            log.info("An outbox message with saga id: {} is already saved to database with notification status: {}!",
                    notificationRequest.getSagaId(),
                    notificationStatus);
            notificationResponseMessagePublisher.publish(documentOutboxMessage,
                    documentOutboxHelper::updateOutboxMessage);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void persistCancelledNotificationOnHistoryRecords(NotificationRequest notificationRequest) {
        log.info("Processing cancellation for notification request: {}", notificationRequest.getDocumentId());

        if (publishIfOutboxMessageProcessedForNotification(notificationRequest, NotificationStatus.NOTIFICATION_CANCELLED)) {
            log.info("Cancellation already processed for saga id: {}", notificationRequest.getSagaId());
            return;
        }

        log.warn("Cancellation logic not fully implemented yet for document id: {}",
                notificationRequest.getDocumentId());
    }
}
