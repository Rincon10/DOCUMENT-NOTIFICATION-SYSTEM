package com.document.notification.system.generator.service.dataaccess.outbox.document.repository;

import com.document.notification.system.domain.valueobject.GenerationStatus;
import com.document.notification.system.generator.service.dataaccess.outbox.document.entity.DocumentOutboxEntity;
import com.document.notification.system.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentOutboxJpaRepository extends JpaRepository<DocumentOutboxEntity, UUID> {

    Optional<List<DocumentOutboxEntity>> findByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);

    Optional<DocumentOutboxEntity> findByTypeAndSagaIdAndGenerationStatusAndOutboxStatus(String type,
                                                                                         UUID sagaId,
                                                                                         GenerationStatus generationStatus,
                                                                                         OutboxStatus outboxStatus);

    void deleteByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);

}
