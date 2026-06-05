package com.dcm.backend.demo.dto;

import lombok.Data;

@Data
public class JobCreateRequest {
    public String dockerImage;
    public String fileUrl;

    public int requiredCpu;
    public int requiredMemoryMB;
    public boolean gpuRequired;
    public String userId;
}
