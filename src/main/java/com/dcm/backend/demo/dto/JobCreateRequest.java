package com.dcm.backend.demo.dto;

public class JobCreateRequest {
    public String dockerImage;
    public String fileUrl;

    public int requiredCpu;
    public int requiredMemoryMB;
    public boolean gpuRequired;
}
