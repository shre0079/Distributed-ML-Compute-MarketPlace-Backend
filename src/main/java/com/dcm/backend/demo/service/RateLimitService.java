package com.dcm.backend.demo.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Separate maps for each endpoint type
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> jobCreateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> depositBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> workerRegisterBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> pollBuckets = new ConcurrentHashMap<>();

    // Login — 5 attempts per minute per IP (brute force protection)
    public Bucket resolveLoginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    // User register — 3 per minute per IP (spam protection)
    public Bucket resolveRegisterBucket(String ip) {
        return registerBuckets.computeIfAbsent(ip, k -> newBucket(3, Duration.ofMinutes(1)));
    }

    // Job create — 10 per minute per userId (abuse protection)
    public Bucket resolveJobCreateBucket(String userId) {
        return jobCreateBuckets.computeIfAbsent(userId, k -> newBucket(10, Duration.ofMinutes(1)));
    }

    // Deposit — 5 per minute per userId
    public Bucket resolveDepositBucket(String userId) {
        return depositBuckets.computeIfAbsent(userId, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    // Worker register — 5 per minute per IP
    public Bucket resolveWorkerRegisterBucket(String ip) {
        return workerRegisterBuckets.computeIfAbsent(ip, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    // Worker poll — 30 per minute per workerId
    public Bucket resolvePollBucket(String workerId) {
        return pollBuckets.computeIfAbsent(workerId, k -> newBucket(30, Duration.ofMinutes(1)));
    }

    // Helper — creates a new bucket with given capacity and refill duration
    private Bucket newBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private final Map<String, Bucket> fileUploadBuckets = new ConcurrentHashMap<>();

    // 10 uploads per minute per user
    public Bucket resolveFileUploadBucket(String userId) {
        return fileUploadBuckets.computeIfAbsent(userId, k -> newBucket(2, Duration.ofMinutes(1)));
    }
}