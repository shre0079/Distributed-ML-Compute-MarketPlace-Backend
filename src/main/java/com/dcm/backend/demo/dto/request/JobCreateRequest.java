package com.dcm.backend.demo.dto.request;

import lombok.Data;

@Data
public class JobCreateRequest {
    public String dockerImage;
    public String fileUrl;
    public int requiredCpu;
    public int requiredMemoryMB;
    public boolean gpuRequired;
    // userId  extracted from JWT now
}
