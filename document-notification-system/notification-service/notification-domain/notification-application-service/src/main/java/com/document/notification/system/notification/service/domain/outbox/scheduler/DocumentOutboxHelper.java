package com.document.notification.system.notification.service.domain.outbox.scheduler;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.domain.utils.JsonSerializationUtil;
import com.document.notification.system.notification.service.domain.exception.NotificationDomainException;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentEventPayload;
import com.document.notification.system.notification.service.domain.outbox.model.DocumentOutboxMessage;
import com.document.notification.system.notification.service.domain.ports.output.repository.DocumentOutboxRepository;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.document.notification.system.saga.constants.SagaConstants.SAGA_NAME;

@Component
@Slf4j
@AllArgsConstructor
public class DocumentOutboxHelper {
    private final DocumentOutboxRepository documentOutboxRepository;

    @Transactional(readOnly = true)
    public Optional<DocumentOutboxMessage> getCompletedDocumentOutboxMessageBySagaIdAndNotificationStatus(UUID sagaId,
                                                                                                          NotificationStatus notificationStatus) {
        return documentOutboxRepository.findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(
                SAGA_NAME, sagaId, notificationStatus, OutboxStatus.COMPLETED);
    }

    /**
     * True si la saga ya tiene fila de outbox en cualquier estado: PENDING (envio en curso), SENT o FAILED
     * (ya procesada). En todos los casos una redelivery se descarta sin volver a enviar.
     */
    @Transactional(readOnly = true)
    public boolean existsDocumentOutboxMessage(UUID sagaId) {
        return documentOutboxRepository.existsByTypeAndSagaId(SAGA_NAME, sagaId);
    }

    /**
     * Claim del envio (patron "reservar antes de actuar"): inserta la fila de outbox en
     * {@code NOTIFICATION_PENDING / STARTED} ANTES de enviar el correo, en transaccion propia con commit
     * inmediato. El indice unico {@code (type, saga_id, notification_status, outbox_status)} garantiza que
     * solo un hilo/replica gana el INSERT; el resto recibe {@link org.springframework.dao.DataIntegrityViolationException}
     * y debe salir sin enviar. Es la unica guarda que corre antes del efecto secundario, por eso es la unica
     * que evita correos duplicados.
     *
     * <p>{@code REQUIRES_NEW}: el claim tiene que ser visible para los demas hilos antes de empezar a enviar,
     * y la excepcion debe salir de aqui para que el llamador la capture sin quedar en rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claimDocumentOutboxMessage(DocumentEventPayload pendingPayload, UUID sagaId) {
        saveDocumentOutboxMessage(pendingPayload, NotificationStatus.NOTIFICATION_PENDING, OutboxStatus.STARTED,
                sagaId);
    }

    /**
     * Cierra el claim tras el envio: la misma fila pasa de PENDING al estado final (SENT/FAILED) con el payload
     * definitivo. Sigue en STARTED, asi que el scheduler la publica en el siguiente tick.
     */
    @Transactional
    public void completeDocumentOutboxMessage(DocumentEventPayload finalPayload,
                                              NotificationStatus finalStatus,
                                              UUID sagaId) {
        DocumentOutboxMessage claimed = documentOutboxRepository.findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(
                        SAGA_NAME, sagaId, NotificationStatus.NOTIFICATION_PENDING, OutboxStatus.STARTED)
                .orElseThrow(() -> new NotificationDomainException(
                        "Claimed (PENDING) outbox message not found for saga id: " + sagaId));

        claimed.setNotificationStatus(finalStatus);
        claimed.setPayload(JsonSerializationUtil.toJson(finalPayload,
                "Could not create DocumentEventPayload for notification id: " + finalPayload.getNotificationId()));
        claimed.setProcessedAt(DateUtils.getZoneDateTimeByUTCZoneId());
        save(claimed);
        log.info("Outbox message id: {} for saga id: {} completed with notification status: {}",
                claimed.getId(), sagaId, finalStatus);
    }

    /**
     * Marca la fila del outbox con el estado indicado.
     *
     * <p>El indice unico {@code (type, saga_id, notification_status, outbox_status)} impide que existan
     * dos filas COMPLETED para la misma saga. Si una redelivery de Kafka dejo una fila STARTED duplicada,
     * al intentar pasarla a COMPLETED choca con la que ya esta COMPLETED y la transaccion falla: el offset
     * no se commitea, Kafka reentrega, y el servicio entra en un bucle infinito sin procesar nada nuevo.
     *
     * <p>Esa colision no es un error: significa que el trabajo <b>ya se hizo</b>. Se descarta la fila
     * duplicada y se continua, que es lo que rompe el bucle.
     */
    @Transactional
    public void updateOutboxMessage(DocumentOutboxMessage documentOutboxMessage, OutboxStatus outboxStatus) {
        documentOutboxMessage.setOutboxStatus(outboxStatus);
        try {
            save(documentOutboxMessage);
            log.info("Document outbox table status is updated as: {}", outboxStatus.name());
        } catch (DataIntegrityViolationException e) {
            log.warn("Outbox message id: {} for saga id: {} could not be set to {}: another row already holds "
                            + "that state. The work was already done, discarding the duplicate row.",
                    documentOutboxMessage.getId(), documentOutboxMessage.getSagaId(), outboxStatus.name());
            documentOutboxRepository.deleteById(documentOutboxMessage.getId());
        }
    }

    @Transactional
    public void saveDocumentOutboxMessage(DocumentEventPayload eventPayload,
                                          NotificationStatus notificationStatus,
                                          OutboxStatus outboxStatus,
                                          UUID sagaId) {
        String payload = JsonSerializationUtil.toJson(eventPayload,
                "Could not create DocumentEventPayload for notification id: " + eventPayload.getNotificationId());

        DocumentOutboxMessage outboxMessage = DocumentOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(DateUtils.getZoneDateTimeByUTCZoneId())
                .type(SAGA_NAME)
                .payload(payload)
                .notificationStatus(notificationStatus)
                .outboxStatus(outboxStatus)
                .build();

        save(outboxMessage);
        log.info("Document outbox message saved with id: {} for saga id: {}", outboxMessage.getId(), sagaId);
    }

    private void save(DocumentOutboxMessage documentOutboxMessage) {
        DocumentOutboxMessage response = documentOutboxRepository.save(documentOutboxMessage);
        if (response == null) {
            log.error("Could not save DocumentOutboxMessage!");
            throw new NotificationDomainException("Could not save DocumentOutboxMessage!");
        }
        log.info("DocumentOutboxMessage is saved with id: {}", documentOutboxMessage.getId());
    }

    /**
     * Excluye NOTIFICATION_PENDING: esas filas son claims de envios en curso, no respuestas publicables.
     */
    public Optional<List<DocumentOutboxMessage>> getDocumentOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        return documentOutboxRepository.findByTypeAndOutboxStatusAndNotificationStatusNot(SAGA_NAME, outboxStatus,
                NotificationStatus.NOTIFICATION_PENDING);
    }
}
