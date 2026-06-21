package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.enums.Priority;
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

    public static BigDecimal getPriorityMultiplier(Priority priority) {
        return switch (priority) {
            case LOW -> PRIORITY_LOW_MULTIPLIER;
            case NORMAL -> PRIORITY_NORMAL_MULTIPLIER;
            case HIGH -> PRIORITY_HIGH_MULTIPLIER;
            case URGENT -> PRIORITY_URGENT_MULTIPLIER;
        };
    }


    // Called at job completion — uses the rate LOCKED into the job at creation time
    public static void calculateBilling(Job job) {
        BigDecimal multiplier = getPriorityMultiplier(job.priority);

        BigDecimal runtimeSeconds = BigDecimal.valueOf(job.durationMs)
                .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);

        BigDecimal cost = runtimeSeconds
                .multiply(job.lockedRatePerSecond)
                .multiply(multiplier)
                .setScale(8, RoundingMode.HALF_UP);

        job.cost = cost;
        job.workerReward = cost.multiply(WORKER_SHARE).setScale(8, RoundingMode.HALF_UP);
        job.platformFee = cost.multiply(PLATFORM_SHARE).setScale(8, RoundingMode.HALF_UP);
    }

    // Called at job creation — rate comes from the TARGETED worker, not a platform constant
    public static BigDecimal calculateEstimate(int maxRuntimeSeconds,
                                               BigDecimal ratePerSecond,
                                               Priority priority) {
        BigDecimal multiplier = getPriorityMultiplier(priority);
        return BigDecimal.valueOf(maxRuntimeSeconds)
                .multiply(ratePerSecond)
                .multiply(multiplier)
                .setScale(8, RoundingMode.HALF_UP);
    }

    // Calculates refund after job completes
    public static BigDecimal calculateRefund(Job job) {
        return job.estimatedCost.subtract(job.cost).setScale(8, RoundingMode.HALF_UP);
    }
}
