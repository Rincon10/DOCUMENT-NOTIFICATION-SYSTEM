package com.document.notification.system.document.repository;

import com.document.notification.system.document.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 * @since 19/11/2025
 */
@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {
}
