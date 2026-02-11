package com.dcm.backend.demo.dto;

import com.dcm.backend.demo.enums.JobStatus;

public class Job {

    public String jobId;
    public String dockerImage;
    public String fileUrl;
    public JobStatus status;

}
