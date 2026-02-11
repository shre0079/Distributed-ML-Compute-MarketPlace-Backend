package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.WorkerInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MockController {

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

        boolean hasJob = Math.random() > 0.5; // 50% chance

        if (!hasJob) {
            return ResponseEntity.noContent().build(); // 204
        }

        return ResponseEntity.ok(
                Map.of(
                        "jobId", "job123",
                        "dockerImage", "hello-world",
                        "fileUrl", "http://localhost:8080/files/input.txt"
                )
        );
    }

    @GetMapping("/files/input.txt")
    public String file() {
        return "Sample training data";
    }

    @PostMapping("/jobs/result")
    public String uploadResult(
            @RequestParam String jobId,
            @RequestBody byte[] body) {

        String logs = new String(body);

        System.out.println("Result for job " + jobId + ":");
        System.out.println(logs);

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

    

}
