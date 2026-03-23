package com.document.notification.system.document.service.dataaccess.outbox.notification.repository;

import com.document.notification.system.document.service.dataaccess.outbox.notification.entity.NotificationOutboxEntity;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Repository
public interface NotificationOutboxJpaRepository extends JpaRepository<NotificationOutboxEntity, UUID> {
    Optional<List<NotificationOutboxEntity>> findByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                                                    OutboxStatus outboxStatus,
                                                                                    List<SagaStatus> sagaStatus);

    Optional<NotificationOutboxEntity> findByTypeAndSagaIdAndSagaStatusIn(String type,
                                                                        UUID sagaId,
                                                                        List<SagaStatus> sagaStatus);

    void deleteByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                    OutboxStatus outboxStatus,
                                                    List<SagaStatus> sagaStatus);


}
