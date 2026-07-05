package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.dto.response.WorkerJobSummaryResponse;
import com.dcm.backend.demo.dto.response.WorkerStatusResponse;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.exception.ConflictException;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.exception.UnauthorizedException;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import com.dcm.backend.demo.scheduler.WorkerHealthScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WorkerService {

    private static final BigDecimal MIN_CPU_RATE = BigDecimal.valueOf(0.0001);
    private static final BigDecimal MAX_CPU_RATE = BigDecimal.valueOf(0.0010);
    private static final BigDecimal MIN_GPU_RATE = BigDecimal.valueOf(0.0005);
    private static final BigDecimal MAX_GPU_RATE = BigDecimal.valueOf(0.0050);


    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkerHealthScheduler workerHealthScheduler;
    private final JobRepository jobRepository;

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);


    public WorkerService(WorkerRepository workerRepository,
                         PasswordEncoder passwordEncoder,
                         WorkerHealthScheduler workerHealthScheduler, JobRepository jobRepository) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.workerHealthScheduler = workerHealthScheduler;
        this.jobRepository = jobRepository;
    }

    public String register(WorkerInfo workerInfo) {

        workerRepository.findById(workerInfo.workerId).ifPresent(existing -> {
            // Worker already exists — validate secret matches
            if (!passwordEncoder.matches(workerInfo.workerSecret,
                    existing.workerSecret)) {
                throw new UnauthorizedException(
                        "Worker already registered with a different secret");
            }
        });

        WorkerInfo worker = workerRepository.findById(workerInfo.workerId)
                .orElse(new WorkerInfo());
        boolean isNewWorker = (worker == null);

        worker.workerId = workerInfo.workerId;
        worker.cpuCores = workerInfo.cpuCores;
        worker.memoryMB = workerInfo.memoryMB;
        worker.os = workerInfo.os;
        worker.hasGpu = workerInfo.hasGpu;
        worker.lastSeen = System.currentTimeMillis();

        // Hash secret on first registration, keep existing hash on re-registration
        if (worker.workerSecret == null) {
            worker.workerSecret = passwordEncoder.encode(workerInfo.workerSecret);
        }

        // Rates only set on FIRST registration — later changes go through PUT /workers/rate
        if (isNewWorker) {
            validateRateBounds(workerInfo.cpuRatePerSecond, workerInfo.gpuRatePerSecond);
            worker.cpuRatePerSecond = workerInfo.cpuRatePerSecond;
            worker.gpuRatePerSecond = workerInfo.gpuRatePerSecond;
        }

        workerRepository.save(worker);
        workerHealthScheduler.workerRegistered(worker.workerId, worker.lastSeen);

        // register
        log.info("Worker registered: {} | CPU: {} | RAM: {} | GPU: {} | cpuRate=${}/s | gpuRate=${}/s",
                worker.workerId, worker.cpuCores, worker.memoryMB, worker.hasGpu,
                worker.cpuRatePerSecond, worker.gpuRatePerSecond);

        return "ok";
    }

    public void validateWorker(String workerId, String workerSecret) {

        WorkerInfo worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        if (!passwordEncoder.matches(workerSecret, worker.workerSecret)) {
            throw new UnauthorizedException("Invalid worker credentials");
        }
    }

    public void heartbeat(String workerId, String workerSecret, String status) {

        validateWorker(workerId, workerSecret);
        workerHealthScheduler.workerHeartbeat(workerId);

        // heartbeat
        log.debug("Heartbeat from {} status={}", workerId, status); // debug, not info — this fires constantly

    }

    public List<WorkerStatusResponse> getAllWorkerStatuses() {

        List<WorkerInfo> workers = workerRepository.findAll();
        long now = System.currentTimeMillis();

        return workers.stream()
                .map(worker -> buildWorkerStatus(worker, now))
                .toList();
    }

    public WorkerStatusResponse getWorkerStatus(String workerId) {

        WorkerInfo worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        return buildWorkerStatus(worker, System.currentTimeMillis());
    }

    private WorkerStatusResponse buildWorkerStatus(WorkerInfo worker, long now) {

        // Check in-memory map first (real-time heartbeat source of truth)
        // Fall back to DB lastSeen if worker not in map (e.g. after restart)
        Long lastHeartbeat = workerHealthScheduler.getLastSeen(worker.workerId);
        long effectiveLastSeen = lastHeartbeat != null ? lastHeartbeat : worker.lastSeen;


//        boolean online = (now - worker.lastSeen) <= 15000;

        boolean online = (now - effectiveLastSeen) <= 15000;

        int activeJobs = jobRepository.countByWorkerIdAndStatus(
                worker.workerId, JobStatus.RUNNING);

        int completedJobs = jobRepository.countByWorkerIdAndStatus(
                worker.workerId, JobStatus.SUCCESS);

//        return new WorkerStatusResponse(
//                worker.workerId,
//                worker.os,
//                worker.cpuCores,
//                worker.memoryMB,
//                worker.hasGpu,
//                online,
//                worker.lastSeen,
//                worker.totalEarned,
//                activeJobs,
//                completedJobs,
//                worker.reputation,
//                worker.cpuRatePerSecond,
//                worker.gpuRatePerSecond,
//                worker.walletBalance
//        );
        return new WorkerStatusResponse(
                worker.workerId,
                worker.os,
                worker.cpuCores,
                worker.memoryMB,
                worker.hasGpu,
                online,
                effectiveLastSeen,
                worker.totalEarned,
                activeJobs,
                completedJobs,
                worker.reputation,
                worker.cpuRatePerSecond,
                worker.gpuRatePerSecond,
                worker.walletBalance
        );
    }

    private static final BigDecimal REPUTATION_SUCCESS = BigDecimal.valueOf(2.0);
    private static final BigDecimal REPUTATION_TIMEOUT = BigDecimal.valueOf(-2.0);
    private static final BigDecimal REPUTATION_FAILED = BigDecimal.valueOf(-5.0);
    private static final BigDecimal REPUTATION_MAX = BigDecimal.valueOf(100.0);
    private static final BigDecimal REPUTATION_MIN = BigDecimal.valueOf(0.0);


    public void adjustReputation(String workerId, BigDecimal delta) {

        WorkerInfo worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) return; // worker may not exist (e.g. already removed)

        BigDecimal newReputation = worker.reputation.add(delta);

        // Clamp between 0 and 100
        if (newReputation.compareTo(REPUTATION_MAX) > 0) {
            newReputation = REPUTATION_MAX;
        } else if (newReputation.compareTo(REPUTATION_MIN) < 0) {
            newReputation = REPUTATION_MIN;
        }

        worker.reputation = newReputation;
        workerRepository.save(worker);

        System.out.println("Worker " + workerId + " reputation: " +
                worker.reputation + " (delta=" + delta + ")");
    }

    public void onJobSuccess(String workerId) {
        adjustReputation(workerId, REPUTATION_SUCCESS);
    }

    public void onJobTimeout(String workerId) {
        adjustReputation(workerId, REPUTATION_TIMEOUT);
    }

    public void onJobFailed(String workerId) {
        adjustReputation(workerId, REPUTATION_FAILED);
    }

    public WorkerInfo updateRates(String workerId, String workerSecret,
                                  BigDecimal cpuRate, BigDecimal gpuRate) {

        validateWorker(workerId, workerSecret);
        validateRateBounds(cpuRate, gpuRate);

        WorkerInfo worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        worker.cpuRatePerSecond = cpuRate;
        worker.gpuRatePerSecond = gpuRate;
        workerRepository.save(worker);

        // updateRates
        log.info("Worker {} updated rates: cpu=${}/s gpu=${}/s", workerId, cpuRate, gpuRate);


        return worker;
    }

    private void validateRateBounds(BigDecimal cpuRate, BigDecimal gpuRate) {

        if (cpuRate == null || gpuRate == null) {
            throw new IllegalArgumentException(
                    "cpuRatePerSecond and gpuRatePerSecond are required");
        }
        if (cpuRate.compareTo(MIN_CPU_RATE) < 0 || cpuRate.compareTo(MAX_CPU_RATE) > 0) {
            throw new IllegalArgumentException(
                    "cpuRatePerSecond must be between $" + MIN_CPU_RATE + " and $" + MAX_CPU_RATE);
        }
        if (gpuRate.compareTo(MIN_GPU_RATE) < 0 || gpuRate.compareTo(MAX_GPU_RATE) > 0) {
            throw new IllegalArgumentException(
                    "gpuRatePerSecond must be between $" + MIN_GPU_RATE + " and $" + MAX_GPU_RATE);
        }
    }

//    public void validateWorker(String workerId, String workerSecret) {
//
//        WorkerInfo worker = workerRepository.findById(workerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Worker not found: " + workerId));
//
//        if (!passwordEncoder.matches(workerSecret, worker.workerSecret)) {
//            throw new UnauthorizedException("Invalid worker credentials");
//        }
//    }

    public boolean isWorkerOnline(String workerId) {
        long now = System.currentTimeMillis();
        Long lastHeartbeat = workerHealthScheduler.getLastSeen(workerId);
        long effectiveLastSeen = lastHeartbeat != null
                ? lastHeartbeat
                : workerRepository.findById(workerId).map(w -> w.lastSeen).orElse(0L);
        return (now - effectiveLastSeen) <= 15000;
    }

    public List<WorkerJobSummaryResponse> getWorkerJobHistory(String workerId, String workerSecret) {

        validateWorker(workerId, workerSecret);

        List<Job> jobs = jobRepository
                .findAllByTargetWorkerIdOrderByCreatedAtDesc(workerId);

        return jobs.stream().map(this::toSummary).toList();
    }

    private WorkerJobSummaryResponse toSummary(Job job) {
        return new WorkerJobSummaryResponse(
                job.jobId,
                job.dockerImage,
                job.status,
                job.priority,
                job.durationMs,
                job.cost,
                job.workerReward,
                job.lockedRatePerSecond,
                job.createdAt
        );
    }


}