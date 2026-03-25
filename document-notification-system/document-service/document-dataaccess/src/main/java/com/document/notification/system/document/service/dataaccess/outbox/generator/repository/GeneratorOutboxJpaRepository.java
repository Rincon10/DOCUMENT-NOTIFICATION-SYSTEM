package com.document.notification.system.document.service.dataaccess.outbox.generator.repository;

import com.document.notification.system.document.service.dataaccess.outbox.generator.entity.GenerationOutboxEntity;
import com.document.notification.system.outbox.OutboxStatus;
import com.document.notification.system.saga.SagaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/02/2026
 */
@Repository
public interface GeneratorOutboxJpaRepository extends JpaRepository<GenerationOutboxEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    Optional<List<GenerationOutboxEntity>> findByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                                                    OutboxStatus outboxStatus,
                                                                                    List<SagaStatus> sagaStatus);

    Optional<GenerationOutboxEntity> findByTypeAndSagaIdAndSagaStatusIn(String type,
                                                                        UUID sagaId,
                                                                        List<SagaStatus> sagaStatus);

    void deleteByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                    OutboxStatus outboxStatus,
                                                    List<SagaStatus> sagaStatus);


}
