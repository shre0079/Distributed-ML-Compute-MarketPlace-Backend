package com.dcm.backend.demo.dto.entity;

import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.enums.Priority;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Job {

    @Id
    public String jobId;

    @Column(nullable = false)
    public String userId;

    public String dockerImage;
    public String fileUrl;

    @Enumerated(EnumType.STRING)
    public JobStatus status;

    public int retryCount;
    public int maxRetries = 3;

    public String workerId;

    @Column(nullable = false)
    public int requiredCpu=1;
    @Column(nullable = false)
    public int requiredMemoryMB=512;
    @Column(nullable = false)
    public boolean gpuRequired=false;

    public Long durationMs;

    @Column(precision = 12, scale = 8)
    public BigDecimal cost;
    @Column(precision = 12, scale = 8)
    public BigDecimal workerReward;
    @Column(precision = 12, scale = 8)
    public BigDecimal platformFee;

    @Column(nullable = false)
    public int maxRuntimeSeconds;

    @Column(precision = 12, scale = 8)
    public BigDecimal estimatedCost;

    @Column(columnDefinition = "TEXT")
    public String logs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Priority priority = Priority.NORMAL;

    @Column(nullable = false)
    public long createdAt = System.currentTimeMillis();

    @Column(nullable = false)
    public String targetWorkerId;       // the specific worker this job is for

    @Column(nullable = false)
    public long expiresAt;              // CREATED jobs expire if not picked up by this time

    @Column(precision = 12, scale = 8)
    public BigDecimal lockedRatePerSecond;  // rate frozen at job creation time

    public boolean hasArtifact = false;

    @Column(nullable = false)
    public boolean networkRequired = false;

    public Job(String jobId, String dockerImage, String fileUrl,
               String userId, int maxRuntimeSeconds, boolean networkRequired) {
        this.jobId = jobId;
        this.dockerImage = dockerImage;
        this.fileUrl = fileUrl;
        this.userId = userId;
        this.maxRuntimeSeconds = maxRuntimeSeconds;
        this.status = JobStatus.CREATED;
        this.retryCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.networkRequired=networkRequired;
    }

    public Job() {}


}