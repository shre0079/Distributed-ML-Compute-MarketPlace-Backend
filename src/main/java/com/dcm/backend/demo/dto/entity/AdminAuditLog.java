package com.dcm.backend.demo.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id
    public String auditId;

    public String adminUserId;
    public String action;     // e.g. "FORCE_FAIL_JOB", "BAN_WORKER"
    public String targetId;   // jobId / workerId / withdrawalId
    public String details;    // optional, e.g. rejection reason
    public long timestamp;
}