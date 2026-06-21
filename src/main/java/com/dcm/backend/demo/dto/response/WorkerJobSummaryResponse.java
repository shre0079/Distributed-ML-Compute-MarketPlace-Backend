package com.dcm.backend.demo.dto.response;

import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.enums.Priority;

import java.math.BigDecimal;

public class WorkerJobSummaryResponse {

    public String jobId;
    public String dockerImage;
    public JobStatus status;
    public Priority priority;
    public Long durationMs;
    public BigDecimal cost;
    public BigDecimal workerReward;
    public BigDecimal lockedRatePerSecond;
    public long createdAt;

    public WorkerJobSummaryResponse(String jobId, String dockerImage, JobStatus status,
                                    Priority priority, Long durationMs, BigDecimal cost,
                                    BigDecimal workerReward, BigDecimal lockedRatePerSecond,
                                    long createdAt) {
        this.jobId = jobId;
        this.dockerImage = dockerImage;
        this.status = status;
        this.priority = priority;
        this.durationMs = durationMs;
        this.cost = cost;
        this.workerReward = workerReward;
        this.lockedRatePerSecond = lockedRatePerSecond;
        this.createdAt = createdAt;
    }
}