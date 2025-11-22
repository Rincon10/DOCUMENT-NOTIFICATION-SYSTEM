package com.document.notification.system.helper;

import com.document.notification.system.document.service.domain.event.DocumentCreatedEvent;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Slf4j
@Component
public class DocumentCreateHelper implements IDocumentCreateHelper {

    @Transactional
    @Override
    public DocumentCreatedEvent persistOrder(CreateDocumentCommand createOrderCommand) {
        return null;
    }
}
