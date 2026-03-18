package com.document.notification.system.document.service.domain.event;

import com.document.notification.system.document.service.domain.entity.Document;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 14/03/2026
 */
@Getter
public class DocumentGeneratedEvent extends DocumentEvent {
    private final String fileName;
    private final String contentType;
    private final String contentBase64;
    private final Long fileSizeInBytes;

    public DocumentGeneratedEvent(Document document, ZonedDateTime createdAt) {
        super(document, createdAt);
        this.fileName = null;
        this.contentType = null;
        this.contentBase64 = null;
        this.fileSizeInBytes = null;
    }

    public DocumentGeneratedEvent(Document document, ZonedDateTime createdAt,
                                  String fileName, String contentType,
                                  String contentBase64, Long fileSizeInBytes) {
        super(document, createdAt);
        this.fileName = fileName;
        this.contentType = contentType;
        this.contentBase64 = contentBase64;
        this.fileSizeInBytes = fileSizeInBytes;
    }
}
