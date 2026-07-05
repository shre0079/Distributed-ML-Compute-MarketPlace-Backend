package com.dcm.backend.demo.dto.request;

import com.dcm.backend.demo.enums.Priority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobCreateRequest {
    @NotBlank(message = "Docker image is required")
    public String dockerImage;

    @NotBlank(message = "File URL is required")
    public String fileUrl;

    @Min(value = 1, message = "requiredCpu must be at least 1")
    public int requiredCpu;

    @Min(value = 128, message = "requiredMemoryMB must be at least 128MB. " +
            "Note: most Docker images need at least 256MB to start.")
    public int requiredMemoryMB;

    public boolean gpuRequired;

    @Min(value = 1, message = "maxRuntimeSeconds must be at least 1")
    @Max(value = 86400, message = "maxRuntimeSeconds cannot exceed 24 hours (86400 seconds)")
    public int maxRuntimeSeconds;
    // userId  extracted from JWT now

    @NotBlank(message = "targetWorkerId is required — choose a worker from GET /workers")
    public String targetWorkerId;

    public Priority priority = Priority.NORMAL;

    public boolean networkRequired = false; // opt-in, defaults to fully isolated
}
