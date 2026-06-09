package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.request.JobCreateRequest;
import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import com.dcm.backend.demo.service.BillingService;
import com.dcm.backend.demo.service.WalletService;
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

    public JobController(UserRepository userRepository, JobRepository jobRepository, WorkerRepository workerRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
        this.walletService = walletService;
    }

    @PostMapping("/jobs/create")
    public Job createJob(@RequestBody JobCreateRequest request) {

        // Extract userId from JWT — not from request body
        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName(); // getName() returns the subject = userId

        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, request.dockerImage, request.fileUrl, userId);
        job.requiredCpu = request.requiredCpu;
        job.requiredMemoryMB = request.requiredMemoryMB;
        job.gpuRequired = request.gpuRequired;
        job.userId = userId;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal estimatedCost = BigDecimal.valueOf(0.01);
        if (user.walletBalance.compareTo(estimatedCost) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        jobRepository.save(job);
        System.out.println("Created job " + jobId + " for user " + userId);
        return job;
    }

    private final WorkerRepository workerRepository;

    @GetMapping("/jobs/poll/{workerId}")
    public synchronized ResponseEntity<?> pollJob(@PathVariable String workerId) {

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
    public String uploadResult(
            @RequestParam String jobId,
            @RequestParam long runtimeMs,
            @RequestBody byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId).orElseThrow();

        // Guard: prevent double-payment if already processed
        if (job.status == JobStatus.SUCCESS) {
            System.out.println("Job " + jobId + " already processed, skipping.");
            return "ok";
        }

        job.durationMs = runtimeMs;

        // Step 1: calculate billing on the in-memory object
        BillingService.calculateBilling(job);

        // Step 2: persist cost/workerReward/platformFee to DB FIRST
        job.status = JobStatus.SUCCESS;
        jobRepository.save(job);

        // Step 3: NOW wallet service can re-fetch job and find cost populated
        walletService.processJobPayment(jobId);

        String logs = new String(body);
        Files.createDirectories(Path.of("results"));
        Files.writeString(Path.of("results", jobId + ".log"), logs);

        System.out.println("Job " + jobId + " SUCCESS");
        System.out.println(
                "Job " + job.jobId +
                        " cost=$" + String.format("%.8f", job.cost) +
                        " workerEarned=$" + String.format("%.8f", job.workerReward) +
                        " platformFee=$" + String.format("%.8f", job.platformFee)
        );

        return "ok";
    }

    @PostMapping("/jobs/fail")
    public String failJob(@RequestParam String jobId) {

        Job job =  jobRepository.findById(jobId).orElse(null);

        if (job != null) {
            job.retryCount++;

            if (job.retryCount < job.maxRetries) {
                job.status = JobStatus.CREATED;
                System.out.println("Retrying job " + jobId + " attempt " + job.retryCount);
            } else {
                job.status = JobStatus.FAILED;
                System.out.println("Job " + jobId + " permanently FAILED");
            }
            jobRepository.save(job);
        }
        return "ok";
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/jobs/status/{status}")
    public List<Job> getByStatus(@PathVariable JobStatus status) {
        return jobRepository.findAllByStatus(status);
    }

    @PostMapping("/jobs/artifact")
    public String uploadArtifact(@RequestParam String jobId,
                                 @RequestBody byte[] body) throws Exception {

        Path dir = Path.of("artifacts");
        Files.createDirectories(dir);

        Path file = dir.resolve(jobId + ".zip");

        Files.write(file, body);

        System.out.println("Saved artifact: " + file);

        return "ok";
    }
}
