package com.dcm.backend.demo.dto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
public class WorkerInfo {

    @Id
    @NotBlank(message = "Worker ID is required")
    public String workerId;

    @Min(value = 1, message = "cpuCores must be at least 1")
    public int cpuCores;

    @Min(value = 128, message = "memoryMB must be at least 128MB")
    public long memoryMB;

    @NotBlank(message = "OS is required")
    public String os;

    public boolean hasGpu;

    public long lastSeen;

    @NotBlank(message = "Worker secret is required")
    @Size(min = 16, message = "Worker secret must be at least 16 characters")
    public String workerSecret;

    @Column(precision = 12, scale = 8)
    public BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 8)
    public BigDecimal totalEarned = BigDecimal.ZERO; // BCrypt hashed
}