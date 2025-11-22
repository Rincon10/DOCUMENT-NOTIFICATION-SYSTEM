package com.document.notification.system.application.rest;

import com.document.notification.system.domain.utils.DateUtils;
import com.document.notification.system.dto.create.CreateDocumentCommand;
import com.document.notification.system.dto.create.CreateDocumentResponse;
import com.document.notification.system.dto.create.DocumentInformation;
import com.document.notification.system.ports.input.service.DocumentApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 9/11/2025
 */
@Slf4j
@RestController
@RequestMapping(value = "/documents", produces = "application/vnd.api.v1+json")
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;

    public DocumentController(DocumentApplicationService documentApplicationService) {
        this.documentApplicationService = documentApplicationService;
    }

    @PostMapping
    public ResponseEntity<CreateDocumentResponse> createOrder(@RequestBody CreateDocumentCommand createDocumentCommand) {
        final DocumentInformation documentInformation = createDocumentCommand.getDocumentInformation();
        String startDate = DateUtils.formatDate(documentInformation.getPeriodStartDate());
        String endDate = DateUtils.formatDate(documentInformation.getPeriodEndDate());

        log.info("Creating document for customer: {} for period: {} --- {}", createDocumentCommand.getCustomerId(),
                startDate, endDate);

        CreateDocumentResponse createDocumentResponse = documentApplicationService.createDocument(createDocumentCommand);
        log.info("Document created with account id: {} ", createDocumentResponse.getAccountId());
        return ResponseEntity.ok(createDocumentResponse);
    }

}
