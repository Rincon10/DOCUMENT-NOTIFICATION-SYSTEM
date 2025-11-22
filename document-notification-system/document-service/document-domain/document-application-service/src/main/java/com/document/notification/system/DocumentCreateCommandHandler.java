package com.document.notification.system;

import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Component
public class DocumentCreateCommandHandler {
    @Transactional
    public CreateDocumentResponse createDocument(CreateDocumentCommand createDocumentCommand){
        CreateDocumentResponse createDocumentResponse = null;

        return createDocumentResponse;

    }

}
