package com.document.notification.system.saga;

import com.document.notification.system.document.service.domain.exception.DocumentDomainException;
import com.document.notification.system.domain.valueobject.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 21/02/2026
 */
@Component
@Slf4j
@AllArgsConstructor
public class DocumentSagaHelper implements IDocumentSagaHelper {

    private static final Map<DocumentStatus, SagaStatus> STATUS_MAPPING = createStatusMapping();

    private static Map<DocumentStatus, SagaStatus> createStatusMapping() {
        Map<DocumentStatus, SagaStatus> map = new EnumMap<>(DocumentStatus.class);
        map.put(DocumentStatus.GENERATED, SagaStatus.PROCESSING);
        map.put(DocumentStatus.SENT, SagaStatus.SUCCESSFUL);
        map.put(DocumentStatus.CANCELLING, SagaStatus.COMPENSATING);
        map.put(DocumentStatus.CANCELLED, SagaStatus.COMPENSATED);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public SagaStatus documentStatusToSagaStatus(DocumentStatus documentStatus) {
        if (Objects.isNull(documentStatus)) {
            log.error("DocumentStatus cannot be null");
            throw new DocumentDomainException("DocumentStatus cannot be null");
        }
        return STATUS_MAPPING.getOrDefault(documentStatus,SagaStatus.STARTED);
    }
}
