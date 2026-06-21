package com.dcm.backend.demo.dto.response;

import java.math.BigDecimal;

public class WorkerStatusResponse {

    public String workerId;
    public String os;
    public int cpuCores;
    public long memoryMB;
    public boolean hasGpu;
    public boolean online;
    public String status;           // "ONLINE" or "OFFLINE"
    public long lastSeenMs;         // raw timestamp
    public String lastSeen;         // human-readable
    public BigDecimal totalEarned;
    public int activeJobs;          // currently RUNNING jobs
    public int completedJobs;
    public BigDecimal reputation;// total SUCCESS jobs
    public BigDecimal cpuRatePerSecond;
    public BigDecimal gpuRatePerSecond;

    public WorkerStatusResponse(String workerId, String os, int cpuCores,
                                long memoryMB, boolean hasGpu, boolean online,
                                long lastSeenMs, BigDecimal totalEarned,
                                int activeJobs, int completedJobs, BigDecimal reputation, BigDecimal cpuRatePerSecond, BigDecimal gpuRatePerSecond) {
        this.workerId = workerId;
        this.os = os;
        this.cpuCores = cpuCores;
        this.memoryMB = memoryMB;
        this.hasGpu = hasGpu;
        this.online = online;
        this.status = online ? "ONLINE" : "OFFLINE";
        this.lastSeenMs = lastSeenMs;
        this.lastSeen = new java.util.Date(lastSeenMs).toString();
        this.totalEarned = totalEarned;
        this.activeJobs = activeJobs;
        this.completedJobs = completedJobs;
        this.reputation=reputation;
        this.cpuRatePerSecond = cpuRatePerSecond;
        this.gpuRatePerSecond = gpuRatePerSecond;
    }
}