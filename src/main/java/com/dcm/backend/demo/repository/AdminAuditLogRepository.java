package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, String> {
}