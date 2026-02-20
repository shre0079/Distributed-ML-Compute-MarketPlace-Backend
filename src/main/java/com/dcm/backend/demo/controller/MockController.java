package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.Job;
import com.dcm.backend.demo.dto.WorkerInfo;
import com.dcm.backend.demo.enums.JobStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MockController {

    private Map<String, Long> workerLastSeen = new ConcurrentHashMap<>();
    private Map<String, Job> jobs = new ConcurrentHashMap<>();

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
    public ResponseEntity<?> pollJob() {

        for (Job job : jobs.values()) {

            if (job.status == JobStatus.CREATED) {
                job.status = JobStatus.RUNNING;  // 🔥 mark running
                return ResponseEntity.ok(job);
            }
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

        Job job = jobs.get(jobId);

        if (job != null) {
            job.status = JobStatus.SUCCESS;
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

            if (now - last > 15000) { // 15 seconds timeout
                System.out.println("Worker DEAD: " + entry.getKey());
            }
        }
    }

    @PostMapping("/jobs/fail")
    public String failJob(@RequestParam String jobId) {

        Job job = jobs.get(jobId);

        if (job != null) {
            job.retryCount++;
            if (job.retryCount < job.maxRetries) {
                job.status = JobStatus.CREATED;
                System.out.println("Retrying job " + jobId +
                        " attempt " + job.retryCount);
            } else {
                job.status = JobStatus.FAILED;
                System.out.println("Job " + jobId + " permanently FAILED");
            }
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

        jobs.put(jobId, job);

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

}
