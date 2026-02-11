package com.dcm.backend.demo.dto;

import com.dcm.backend.demo.enums.JobStatus;

public class Job {

    public String jobId;
    public String dockerImage;
    public String fileUrl;
    public JobStatus status;

    public Job(String id, String image, String url) {
        this.jobId = id;
        this.dockerImage = image;
        this.fileUrl = url;
        this.status = JobStatus.CREATED;
    }
}
