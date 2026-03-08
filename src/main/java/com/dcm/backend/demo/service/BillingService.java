package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.Job;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillingService {

    private static final double CPU_RATE = 0.0002; // per second
    private static final double GPU_RATE = 0.001;  // per second

    private static final double WORKER_SHARE = 0.7;
    private static final double PLATFORM_SHARE = 0.3;

    public static void calculateBilling(Job job) {

        BigDecimal runtime = BigDecimal.valueOf(job.durationMs)
                .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);

        BigDecimal rate = job.gpuRequired
                ? BigDecimal.valueOf(GPU_RATE)
                : BigDecimal.valueOf(CPU_RATE);

        BigDecimal cost = runtime.multiply(rate);

        job.cost = cost.setScale(8, RoundingMode.HALF_UP);
        job.workerReward = cost.multiply(BigDecimal.valueOf(0.7))
                .setScale(8, RoundingMode.HALF_UP);
        job.platformFee = cost.multiply(BigDecimal.valueOf(0.3))
                .setScale(8, RoundingMode.HALF_UP);
    }
}
