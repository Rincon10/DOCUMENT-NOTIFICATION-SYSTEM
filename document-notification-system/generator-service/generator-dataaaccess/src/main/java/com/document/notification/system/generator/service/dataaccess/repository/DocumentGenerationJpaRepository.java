package com.document.notification.system.generator.service.dataaccess.repository;

import com.document.notification.system.generator.service.dataaccess.entity.DocumentGenerationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentGenerationJpaRepository extends JpaRepository<DocumentGenerationEntity, UUID> {

}
