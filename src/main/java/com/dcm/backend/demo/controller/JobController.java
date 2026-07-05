package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.request.JobCreateRequest;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.service.JobService;
import com.dcm.backend.demo.service.RateLimitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class JobController {

    private final JobService jobService;
    private final RateLimitService rateLimitService;
    private final WorkerService workerService;
    private final JobRepository jobRepository;

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    public JobController(JobService jobService,
                         RateLimitService rateLimitService) {
        this.jobService = jobService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/jobs/create")
    public Job createJob(@Valid @RequestBody JobCreateRequest request) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        if (!rateLimitService.resolveJobCreateBucket(userId).tryConsume(1)) {
            throw new RateLimitException(
                    "Too many job creation requests. Try again in a minute.");
        }

        return jobService.createJob(request, userId);
    }

    @GetMapping("/jobs/poll/{workerId}")
    public ResponseEntity<?> pollJob(
            @PathVariable String workerId,
            @RequestParam String workerSecret) {

        if (!rateLimitService.resolvePollBucket(workerId).tryConsume(1)) {
            throw new RateLimitException("Polling too frequently.");
        }

        Job job = jobService.pollJob(workerId);
        return job != null
                ? ResponseEntity.ok(job)
                : ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/result")
    public String uploadResult(
            @RequestParam String jobId,
            @RequestParam long runtimeMs,
            @RequestParam String workerSecret,
            @RequestBody byte[] body) throws Exception {

        jobService.processResult(jobId, runtimeMs, workerSecret, body);
        return "ok";
    }

    @PostMapping("/jobs/fail")
    public String failJob(
            @RequestParam String jobId,
            @RequestParam String workerSecret) {

        jobService.processFailure(jobId, workerSecret);
        return "ok";
    }

    @PostMapping("/jobs/timeout")
    public String timeoutJob(
            @RequestParam String jobId,
            @RequestParam long runtimeMs,
            @RequestParam String workerSecret,
            @RequestBody byte[] body) throws Exception {

        jobService.processTimeout(jobId, runtimeMs, workerSecret, body);
        return "ok";
    }

    @GetMapping("/jobs/{jobId}")
    public Job getJob(@PathVariable String jobId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return jobService.getJobById(jobId, userId);
    }

    @GetMapping("/jobs/{jobId}/logs")
    public ResponseEntity<Map<String, Object>> getJobLogs(
            @PathVariable String jobId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return ResponseEntity.ok(jobService.getJobLogs(jobId, userId));
    }

//    @GetMapping("/jobs")
//    public List<Job> getAllJobs() {
//        return jobService.getAllJobs();
//    }

//    @GetMapping("/jobs/status/{status}")
//    public List<Job> getJobsByStatus(@PathVariable JobStatus status) {
//        return jobService.getJobsByStatus(status);
//    }

    @PostMapping("/jobs/artifact")
    public String uploadArtifact(
            @RequestParam String jobId,
            @RequestParam String workerSecret,
            @RequestBody byte[] body) throws Exception {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        workerService.validateWorker(job.workerId, workerSecret);

        Path dir = Path.of("artifacts");
        Files.createDirectories(dir);
        Files.write(dir.resolve(jobId + ".zip"), body);

        job.hasArtifact = true;   // ← new
        jobRepository.save(job);

        log.info("Saved artifact for job: {}", jobId);
        return "ok";
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Job> cancelJob(@PathVariable String jobId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Job job = jobService.cancelJob(jobId, userId);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/jobs")
    public Page<Job> getAllJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return jobService.getAllJobsForUser(userId, pageable);
    }

    @GetMapping("/jobs/status/{status}")
    public Page<Job> getJobsByStatus(
            @PathVariable JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return jobService.getJobsByStatusForUser(userId, status, pageable);
    }
}