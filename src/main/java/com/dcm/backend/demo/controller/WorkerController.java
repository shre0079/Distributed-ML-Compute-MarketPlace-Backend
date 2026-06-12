package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.service.RateLimitService;
import com.dcm.backend.demo.service.WorkerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class WorkerController {

    private final WorkerService workerService;
    private final RateLimitService rateLimitService;

    public WorkerController(WorkerService workerService, RateLimitService rateLimitService) {
        this.workerService = workerService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    public String register(@Valid
                               @RequestBody WorkerInfo workerInfo, HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        if (!rateLimitService.resolveWorkerRegisterBucket(ip).tryConsume(1)) {
            throw new RateLimitException(
                    "Too many worker registration attempts. Try again in a minute.");
        }

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

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}