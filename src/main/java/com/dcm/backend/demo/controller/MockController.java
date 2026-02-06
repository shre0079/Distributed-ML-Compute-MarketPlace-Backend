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
}
