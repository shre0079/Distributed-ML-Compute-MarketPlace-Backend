package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.request.JobCreateRequest;
import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.exception.InsufficientBalanceException;
import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import com.dcm.backend.demo.service.BillingService;
import com.dcm.backend.demo.service.RateLimitService;
import com.dcm.backend.demo.service.WalletService;
import com.dcm.backend.demo.service.WorkerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

//create, poll, result, fail, status
@RestController
public class JobController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final WorkerService workerService;
    private final RateLimitService rateLimitService;

    public JobController(UserRepository userRepository, JobRepository jobRepository, WorkerService workerService, WorkerRepository workerRepository, WalletService walletService, RateLimitService rateLimitService, RateLimitService rateLimitService1) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.workerService = workerService;
        this.workerRepository = workerRepository;
        this.walletService = walletService;
        this.rateLimitService = rateLimitService1;
    }

    @PostMapping("/jobs/create")
    public Job createJob(@Valid
            @RequestBody JobCreateRequest request) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        // Rate limit per user
        if (!rateLimitService.resolveJobCreateBucket(userId).tryConsume(1)) {
            throw new RateLimitException(
                    "Too many job creation requests. Try again in a minute.");
        }


        // Calculate estimate upfront
        BigDecimal estimatedCost = BillingService.calculateEstimate(
                request.maxRuntimeSeconds, request.gpuRequired);

        // Check balance before creating job
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.walletBalance.compareTo(estimatedCost) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Required: $"
                    + estimatedCost + " Available: $" + user.walletBalance);
        }

        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, request.dockerImage,
                request.fileUrl, userId, request.maxRuntimeSeconds);
        job.requiredCpu = request.requiredCpu;
        job.requiredMemoryMB = request.requiredMemoryMB;
        job.gpuRequired = request.gpuRequired;
        job.estimatedCost = estimatedCost;

        jobRepository.save(job);

        // Pre-deduct estimated cost immediately
        walletService.holdJobEstimate(job, user);

        System.out.println("Created job " + jobId +
                " | estimate=$" + estimatedCost +
                " | maxRuntime=" + request.maxRuntimeSeconds + "s");

        return job;
    }

    private final WorkerRepository workerRepository;

    @GetMapping("/jobs/poll/{workerId}")
    public synchronized ResponseEntity<?> pollJob(@Valid @PathVariable String workerId,@Valid  @RequestParam String workerSecret) {


        // Rate limit per worker
        if (!rateLimitService.resolvePollBucket(workerId).tryConsume(1)) {
            throw new RateLimitException(
                    "Polling too frequently. Slow down.");
        }

        workerService.validateWorker(workerId, workerSecret);

        WorkerInfo worker = workerRepository.findById(workerId).orElse(null);

        if (worker == null) return ResponseEntity.noContent().build();

        List<Job> jobs = jobRepository.findAllByStatus(JobStatus.CREATED);

        System.out.println("Worker " + workerId + " hasGpu=" + worker.hasGpu);
        for (Job job : jobs) {

            boolean compatible =
                    worker.cpuCores >= job.requiredCpu &&
                            worker.memoryMB >= job.requiredMemoryMB &&
                            (!job.gpuRequired || worker.hasGpu);

            if (compatible) {

                job.status = JobStatus.RUNNING;
                job.workerId = workerId;
                jobRepository.save(job);

                return ResponseEntity.ok(job);
            }
        }
        return ResponseEntity.noContent().build();
    }

    private final WalletService walletService;

    @PostMapping("/jobs/result")
    public String uploadResult( @Valid
            @RequestParam String jobId,
                                @Valid
            @RequestParam long runtimeMs,
                                @Valid
            @RequestParam String workerSecret,
                                @Valid
            @RequestBody byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId).orElseThrow(
                () -> new ResourceNotFoundException("Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);

        if (job.status == JobStatus.SUCCESS) {
            System.out.println("Job " + jobId + " already processed, skipping.");
            return "ok";
        }

        job.durationMs = runtimeMs;

        // Calculate actual billing
        BillingService.calculateBilling(job);
        job.status = JobStatus.SUCCESS;
        jobRepository.save(job);

        // Process payment — refunds difference, pays worker actual
        walletService.processJobPayment(jobId);

        String logs = new String(body);
        Files.createDirectories(Path.of("results"));
        Files.writeString(Path.of("results", jobId + ".log"), logs);

        System.out.println("Job " + jobId + " SUCCESS" +
                " | actual=$" + job.cost +
                " | estimate=$" + job.estimatedCost);

        return "ok";
    }

    @PostMapping("/jobs/fail")
    public String failJob(@Valid @RequestParam String jobId, @Valid @RequestParam String workerSecret) {



        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);


        if (job != null) {
            job.retryCount++;

            if (job.retryCount < job.maxRetries) {
                job.status = JobStatus.CREATED;
                System.out.println("Retrying job " + jobId +
                        " attempt " + job.retryCount);
            } else {
                job.status = JobStatus.FAILED;
                // Full refund on permanent failure
                walletService.processFailureRefund(jobId);
                System.out.println("Job " + jobId + " permanently FAILED — refunded");
            }
            jobRepository.save(job);
        }
        return "ok";
    }

    @GetMapping("/jobs/{jobId}/logs")
    public ResponseEntity<Map<String, Object>> getJobLogs(
            @PathVariable String jobId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return ResponseEntity.ok(jobService.getJobLogs(jobId, userId));
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/jobs/status/{status}")
    public List<Job> getByStatus(@Valid @PathVariable JobStatus status) {
        return jobRepository.findAllByStatus(status);
    }

    @PostMapping("/jobs/artifact")
    public String uploadArtifact(@Valid @RequestParam String jobId,
                                 @Valid
                                 @RequestParam String workerSecret,
                                 @Valid
                                 @RequestBody byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);


        Path dir = Path.of("artifacts");
        Files.createDirectories(dir);

        Path file = dir.resolve(jobId + ".zip");

        Files.write(file, body);

        System.out.println("Saved artifact: " + file);

        return "ok";
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Job> cancelJob(@PathVariable String jobId) {

            System.out.println("Job " + jobId + " TIMEOUT" +
                    " | charged=$" + job.estimatedCost);
        }
        return "ok";
    }


    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
