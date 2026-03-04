package com.dcm.backend.demo.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class WorkerInfo {

    @Id
    public String workerId;
    public int cpuCores;
    public long memoryMB;
    public String os;
    public boolean hasGpu;

    public long lastSeen;
}