package com.project.fraudsystem.audit.repository;

import com.project.fraudsystem.audit.model.RagAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RagAuditLogRepository extends JpaRepository<RagAuditLog, Long> {
}