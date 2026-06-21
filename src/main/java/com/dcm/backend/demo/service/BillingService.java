package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Job;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillingService {

    private static final double CPU_RATE = 0.0002; // per second
    private static final double GPU_RATE = 0.001;  // per second

    private static final BigDecimal WORKER_SHARE = BigDecimal.valueOf(0.7);
    private static final BigDecimal PLATFORM_SHARE = BigDecimal.valueOf(0.3);

    private static final BigDecimal PRIORITY_LOW_MULTIPLIER = BigDecimal.valueOf(0.8);
    private static final BigDecimal PRIORITY_NORMAL_MULTIPLIER = BigDecimal.valueOf(1.0);
    private static final BigDecimal PRIORITY_HIGH_MULTIPLIER = BigDecimal.valueOf(1.5);
    private static final BigDecimal PRIORITY_URGENT_MULTIPLIER = BigDecimal.valueOf(2.0);

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

    // Called at job creation to calculate pre-deduction amount
    public static BigDecimal calculateEstimate(int maxRuntimeSeconds, boolean gpuRequired) {
        BigDecimal rate = BigDecimal.valueOf(gpuRequired ? GPU_RATE : CPU_RATE);
        return BigDecimal.valueOf(maxRuntimeSeconds)
                .multiply(rate)
                .setScale(8, RoundingMode.HALF_UP);
    }

    // Calculates refund after job completes
    public static BigDecimal calculateRefund(Job job) {
        return job.estimatedCost
                .subtract(job.cost)
                .setScale(8, RoundingMode.HALF_UP);
    }
}
