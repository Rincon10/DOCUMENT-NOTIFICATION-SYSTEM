package com.document.notification.system.document.entity;

import lombok.*;

import java.io.Serializable;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 22/11/2025
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentItemEntityId implements Serializable {
    private Long documentId;
    private DocumentEntity document;

}
