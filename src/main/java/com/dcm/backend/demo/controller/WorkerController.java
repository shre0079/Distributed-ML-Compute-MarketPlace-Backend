package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping("/register")
    public String register(@Valid
                               @RequestBody WorkerInfo workerInfo) {
        return workerService.register(workerInfo);
    }

    @PostMapping("/workers/heartbeat")
    public String heartbeat(
            @Valid @RequestBody Map<String, String> data) {
        workerService.heartbeat(
                data.get("workerId"),
                data.get("workerSecret"),
                data.get("status")
        );
        return "ok";
    }
}