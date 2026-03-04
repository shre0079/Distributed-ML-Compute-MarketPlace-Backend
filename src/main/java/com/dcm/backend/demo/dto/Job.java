package com.dcm.backend.demo.dto;

import com.dcm.backend.demo.enums.JobStatus;
import jakarta.persistence.*;

@Entity
public class Job {

    @Id
    public String jobId;

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

    public Job(String id, String image, String url) {
        this.jobId = id;
        this.dockerImage = image;
        this.fileUrl = url;
        this.status = JobStatus.CREATED;
        this.retryCount = 0;
    }

    public Job() {
    }
}