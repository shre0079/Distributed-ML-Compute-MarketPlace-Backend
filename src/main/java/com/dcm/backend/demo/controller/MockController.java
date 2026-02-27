package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.Job;
import com.dcm.backend.demo.dto.JobCreateRequest;
import com.dcm.backend.demo.dto.WorkerInfo;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MockController {

    private final JobRepository jobRepository;
    private Map<String, Long> workerLastSeen = new ConcurrentHashMap<>();

    public MockController(JobRepository jobRepository, JobRepository jobRepository1) {
        this.jobRepository = jobRepository1;
    }

//    @PostConstruct
//    public void init() {
//        for (int i = 1; i <= 10; i++) {
//            String id = "job" + i;
//            jobs.put(id,
//                    new Job(id, "hello-world", "http://localhost:8080/files/input.txt"));
//        }
//    }


    @GetMapping("/ping")
    public String ping() {
        return "Hello Agent";
    }

    @PostMapping("/register")
    public String register(@RequestBody WorkerInfo worker) {

        System.out.println("Worker registered: " + worker.workerId +
                " | CPU: " + worker.cpuCores +
                " | RAM: " + worker.memoryMB +
                " | OS: " + worker.os);

        return "ok";
    }

    @GetMapping("/jobs/poll")
    public synchronized ResponseEntity<?> pollJob() {

        Optional<Job> jobOpt = jobRepository.findFirstByStatus(JobStatus.CREATED);

            if (jobOpt.isPresent()) {
                Job job = jobOpt.get();
                job.status = JobStatus.RUNNING;
                jobRepository.save(job);
                return ResponseEntity.ok(job);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/input.txt")
    public String file() {
        return "Sample training data";
    }

    @PostMapping("/jobs/result")
    public String uploadResult(
            @RequestParam String jobId,
            @RequestBody byte[] body) throws Exception {

        Job job = (Job) jobRepository.findById(jobId).orElse(null);

        if (job != null) {
            job.status = JobStatus.SUCCESS;
            jobRepository.save(job);
        }

        String logs = new String(body);

        Files.createDirectories(Path.of("results"));
        Files.writeString(Path.of("results", jobId + ".log"), logs);

        System.out.println("Job " + jobId + " SUCCESS");

        return "ok";
    }

    @PostMapping("/workers/heartbeat")
    public String heartbeat(@RequestBody Map<String, String> data) {

        String workerId = data.get("workerId");
        workerLastSeen.put(workerId, System.currentTimeMillis());

        System.out.println(
                "Heartbeat from " + data.get("workerId") +
                        " status=" + data.get("status")
        );

        return "ok";
    }

    @Scheduled(fixedRate = 5000)
    public void checkDeadWorkers() {

        long now = System.currentTimeMillis();

        for (var entry : workerLastSeen.entrySet()) {
            long last = entry.getValue();

            if (now - last > 15000) {

                System.out.println("Worker DEAD: " + workerId);
                
                List<Job> stuckJobs =
                        jobRepository.findAllByWorkerIdAndStatus(workerId, JobStatus.RUNNING);

                for (Job job : stuckJobs) {
                    job.status = JobStatus.CREATED;
                    job.workerId = null;
                    jobRepository.save(job);

                    System.out.println("Requeued job " + job.jobId);
                }

                workerLastSeen.remove(workerId);
            }
        }
    }

    @PostMapping("/jobs/fail")
    public String failJob(@RequestParam String jobId) {

        Job job = (Job) jobRepository.findById(jobId).orElse(null);

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

    @PostMapping("/jobs/create")
    public Job createJob(@RequestBody JobCreateRequest request) {

        String jobId = UUID.randomUUID().toString();

        Job job = new Job(
                jobId,
                request.dockerImage,
                request.fileUrl
        );

        jobRepository.save(job);

        System.out.println("Created job " + jobId);

        return job;
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
