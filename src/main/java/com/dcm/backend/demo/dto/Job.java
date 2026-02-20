package com.dcm.backend.demo.dto;

import com.dcm.backend.demo.enums.JobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

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