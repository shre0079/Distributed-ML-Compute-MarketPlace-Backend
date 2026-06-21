package com.dcm.backend.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class RateUpdateRequest {

    @NotNull(message = "cpuRatePerSecond is required")
    public BigDecimal cpuRatePerSecond;

    @NotNull(message = "gpuRatePerSecond is required")
    public BigDecimal gpuRatePerSecond;
}