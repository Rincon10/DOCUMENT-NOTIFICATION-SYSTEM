package com.document.notification.system.notification.service.dataaccess.outbox.document.repository;

import com.document.notification.system.notification.service.dataaccess.outbox.document.entity.DocumentOutboxEntity;
import com.document.notification.system.notification.service.domain.valueobject.NotificationStatus;
import com.document.notification.system.outbox.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentOutboxJpaRepository extends JpaRepository<DocumentOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    Optional<List<DocumentOutboxEntity>> findByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);

    Optional<DocumentOutboxEntity> findByTypeAndSagaIdAndNotificationStatusAndOutboxStatus(String type,
                                                                                            UUID sagaId,
                                                                                            NotificationStatus notificationStatus,
                                                                                            OutboxStatus outboxStatus);

    /**
     * True si ya existe una fila para esa saga y estado de notificacion, sin importar el outbox_status.
     * Sirve para detectar trabajo YA en curso (STARTED) y no crear un duplicado que despues no podra
     * pasar a COMPLETED por el indice unico (type, saga_id, notification_status, outbox_status).
     */
    boolean existsByTypeAndSagaIdAndNotificationStatus(String type, UUID sagaId,
                                                      NotificationStatus notificationStatus);

    void deleteByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);
}
