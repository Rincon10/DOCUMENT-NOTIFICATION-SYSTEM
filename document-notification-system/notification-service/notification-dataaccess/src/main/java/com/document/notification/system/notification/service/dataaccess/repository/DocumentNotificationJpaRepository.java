package com.document.notification.system.notification.service.dataaccess.repository;

import com.document.notification.system.notification.service.dataaccess.entity.DocumentNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentNotificationJpaRepository extends JpaRepository<DocumentNotificationEntity, UUID> {
}
