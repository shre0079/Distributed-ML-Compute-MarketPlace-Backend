package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.WorkerInfo;
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

        return ResponseEntity.ok(Map.of("dockerImage", "hello-world"));
    }
}
