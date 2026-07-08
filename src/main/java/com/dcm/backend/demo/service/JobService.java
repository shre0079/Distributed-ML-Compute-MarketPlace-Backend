package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.dto.request.JobCreateRequest;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.exception.InsufficientBalanceException;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.exception.UnauthorizedException;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final WalletService walletService;
    private final WorkerService workerService;
    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    public JobService(JobRepository jobRepository,
                      UserRepository userRepository,
                      WorkerRepository workerRepository,
                      WalletService walletService,
                      WorkerService workerService) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.workerRepository = workerRepository;
        this.walletService = walletService;
        this.workerService = workerService;
    }

    @Transactional
    public Job createJob(JobCreateRequest request, String userId) {

        WorkerInfo targetWorker = workerRepository.findById(request.targetWorkerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + request.targetWorkerId));

        if (!workerService.isWorkerOnline(request.targetWorkerId)) {
            throw new IllegalArgumentException(
                    "Worker " + request.targetWorkerId + " is currently offline");
        }

        boolean compatible =
                targetWorker.cpuCores >= request.requiredCpu &&
                        targetWorker.memoryMB >= request.requiredMemoryMB &&
                        (!request.gpuRequired || targetWorker.hasGpu);

        if (!compatible) {
            throw new IllegalArgumentException(
                    "Worker " + request.targetWorkerId + " does not meet job requirements");
        }

        BigDecimal baseRate = request.gpuRequired
                ? targetWorker.gpuRatePerSecond
                : targetWorker.cpuRatePerSecond;

        BigDecimal estimatedCost = BillingService.calculateEstimate(
                request.maxRuntimeSeconds, baseRate, request.priority);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.walletBalance.compareTo(estimatedCost) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Required: $" + estimatedCost
                            + " Available: $" + user.walletBalance);
        }

        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, request.dockerImage, request.fileUrl,
                userId, request.maxRuntimeSeconds, request.networkRequired);
        job.requiredCpu = request.requiredCpu;
        job.requiredMemoryMB = request.requiredMemoryMB;
        job.gpuRequired = request.gpuRequired;
        job.estimatedCost = estimatedCost;
        job.priority = request.priority;
        job.targetWorkerId = request.targetWorkerId;
        job.lockedRatePerSecond = baseRate;
        job.expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 min window
        job.networkRequired = request.networkRequired;

        jobRepository.save(job);
        walletService.holdJobEstimate(job, user);

        // createJob
        log.info("Created job {} | target={} | lockedRate=${}/s | estimate=${}",
                jobId, request.targetWorkerId, baseRate, estimatedCost);

        return job;
    }


    public synchronized Job pollJob(String workerId) {

        WorkerInfo worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) return null;

        List<Job> jobs = jobRepository
                .findAllByTargetWorkerIdAndStatus(workerId, JobStatus.CREATED);

        jobs.sort((a, b) -> {
            int p = Integer.compare(b.priority.weight, a.priority.weight);
            return p != 0 ? p : Long.compare(a.createdAt, b.createdAt);
        });

        for (Job job : jobs) {
            boolean compatible =
                    worker.cpuCores >= job.requiredCpu &&
                            worker.memoryMB >= job.requiredMemoryMB &&
                            (!job.gpuRequired || worker.hasGpu);

            if (compatible) {
                job.status = JobStatus.RUNNING;
                job.workerId = workerId;
                jobRepository.save(job);
                return job;
            }
        }
        return null;
    }

    @Transactional
    public void processResult(String jobId, long runtimeMs,
                              String workerSecret, byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);

        if (job.status == JobStatus.SUCCESS) return;

        String logs = new String(body);
        job.durationMs = runtimeMs;
        job.logs = logs;

        BillingService.calculateBilling(job);
        job.status = JobStatus.SUCCESS;
        jobRepository.save(job);

        walletService.processJobPayment(jobId);

        // ← new — reward reputation on success
        workerService.onJobSuccess(job.workerId);

        Files.createDirectories(Path.of("results"));
        Files.writeString(Path.of("results", jobId + ".log"), logs);

        // processResult
        log.info("Job {} SUCCESS | actual=${}", jobId, job.cost);

    }

    @Transactional
    public void processFailure(String jobId, String workerSecret) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);

        job.retryCount++;

        if (job.retryCount < job.maxRetries) {
            job.status = JobStatus.CREATED;
            // processFailure — retry branch
            log.info("Retrying job {} attempt {}", jobId, job.retryCount);

        } else {
            job.status = JobStatus.FAILED;
            walletService.processFailureRefund(jobId);

            // ← new — penalize reputation on permanent failure
            workerService.onJobFailed(job.workerId);

            // processFailure — terminal branch
            log.warn("Job {} permanently FAILED — refunded", jobId);

        }
        jobRepository.save(job);
    }

    @Transactional
    public void processTimeout(String jobId, long runtimeMs,
                               String workerSecret, byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);

        if (job.status == JobStatus.TIMEOUT) return;

        String logs = new String(body);
        job.logs = logs;
        job.durationMs = runtimeMs;
        job.status = JobStatus.TIMEOUT;
        jobRepository.save(job);

        walletService.processTimeoutPayment(jobId);

        // ← new — small penalty on timeout
        workerService.onJobTimeout(job.workerId);

        // processTimeout
        log.warn("Job {} TIMEOUT", jobId);
    }

    public Map<String, Object> getJobLogs(String jobId, String userId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        if (!job.userId.equals(userId)) {
            throw new UnauthorizedException(
                    "You don't have access to this job");
        }

        if (job.logs == null || job.logs.isBlank()) {
            throw new ResourceNotFoundException(
                    "No logs available for job: " + jobId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.jobId);
        response.put("status", job.status);
        response.put("logs", job.logs);
        return response;
    }

    public Job getJobById(String jobId, String userId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        if (!job.userId.equals(userId)) {
            throw new UnauthorizedException(
                    "You don't have access to this job");
        }

        return job;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public List<Job> getJobsByStatus(JobStatus status) {
        return jobRepository.findAllByStatus(status);
    }

    @Transactional
    public Job cancelJob(String jobId, String userId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        // Only owner can cancel
        if (!job.userId.equals(userId)) {
            throw new UnauthorizedException(
                    "You don't have permission to cancel this job");
        }

        // Can only cancel CREATED jobs
        if (job.status != JobStatus.CREATED) {
            throw new IllegalArgumentException(
                    "Cannot cancel job with status: " + job.status +
                            ". Only CREATED jobs can be cancelled.");
        }

        job.status = JobStatus.CANCELLED;
        jobRepository.save(job);

        // Full refund
        walletService.processJobCancellation(jobId);

        System.out.println("Job " + jobId + " cancelled by user " + userId);

        return job;
    }

    public Page<Job> getAllJobsForUser(String userId, Pageable pageable) {
        return jobRepository.findAllByUserId(userId, pageable);
    }

    public Page<Job> getJobsByStatusForUser(String userId, JobStatus status, Pageable pageable) {
        return jobRepository.findAllByUserIdAndStatus(userId, status, pageable);
    }

    public Job getJobForArtifactDownload(String jobId, String userId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.userId.equals(userId)) {
            throw new UnauthorizedException("You don't have access to this job");
        }

        if (!job.hasArtifact) {
            throw new ResourceNotFoundException("No artifact available for job: " + jobId);
        }

        return job;
    }

    private void validateReportedRuntime(Job job, long runtimeMs) {

        long now = System.currentTimeMillis();
        long serverElapsedMs = now - job.runningStartedAt;
        long maxAllowedMs = (long) job.maxRuntimeSeconds * 1000;
        long buffer = 5000; // grace for clock skew + network delivery delay

        // Billing is calculated directly from runtimeMs — this is the one
        // check that actually protects against a worker inflating their
        // reported runtime to extract a bigger payout than they earned.
        if (runtimeMs > serverElapsedMs + buffer || runtimeMs > maxAllowedMs + buffer) {
            throw new IllegalArgumentException(
                    "Reported runtime is not plausible for this job.");
        }
    }
}