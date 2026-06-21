package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.WithdrawalRequest;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.dto.request.RateUpdateRequest;
import com.dcm.backend.demo.dto.request.WithdrawalRequestDto;
import com.dcm.backend.demo.dto.response.WorkerJobSummaryResponse;
import com.dcm.backend.demo.dto.response.WorkerStatusResponse;
import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.repository.WithdrawalRepository;
import com.dcm.backend.demo.service.RateLimitService;
import com.dcm.backend.demo.service.WalletService;
import com.dcm.backend.demo.service.WorkerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WorkerController {

    private final WorkerService workerService;
    private final RateLimitService rateLimitService;
    private final WalletService walletService;
    private final WithdrawalRepository withdrawalRepository;

    public WorkerController(WorkerService workerService, RateLimitService rateLimitService, WalletService walletService, WithdrawalRepository withdrawalRepository) {
        this.workerService = workerService;
        this.rateLimitService = rateLimitService;
        this.walletService = walletService;
        this.withdrawalRepository = withdrawalRepository;
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

    // All workers with status — public endpoint, useful for marketplace
    @GetMapping("/workers")
    public ResponseEntity<List<WorkerStatusResponse>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkerStatuses());
    }

    // Specific worker details
    @GetMapping("/workers/{workerId}")
    public ResponseEntity<WorkerStatusResponse> getWorker(
            @PathVariable String workerId) {
        return ResponseEntity.ok(workerService.getWorkerStatus(workerId));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/workers/withdraw")
    public ResponseEntity<WithdrawalRequest> requestWithdrawal(
            @RequestParam String workerId,
            @RequestParam String workerSecret,
            @Valid @RequestBody WithdrawalRequestDto request) {

        workerService.validateWorker(workerId, workerSecret);

        WithdrawalRequest withdrawal = walletService.requestWithdrawal(
                workerId, request.amount);

        return ResponseEntity.ok(withdrawal);
    }

    @GetMapping("/workers/{workerId}/withdrawals")
    public ResponseEntity<List<WithdrawalRequest>> getWorkerWithdrawals(
            @PathVariable String workerId,
            @RequestParam String workerSecret) {

        workerService.validateWorker(workerId, workerSecret);

        return ResponseEntity.ok(
                withdrawalRepository.findAllByWorkerIdOrderByRequestedAtDesc(workerId));
    }

    @PutMapping("/workers/rate")
    public ResponseEntity<WorkerInfo> updateRate(
            @RequestParam String workerId,
            @RequestParam String workerSecret,
            @Valid @RequestBody RateUpdateRequest request) {

        WorkerInfo updated = workerService.updateRates(
                workerId, workerSecret,
                request.cpuRatePerSecond, request.gpuRatePerSecond);

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/workers/{workerId}/jobs")
    public ResponseEntity<List<WorkerJobSummaryResponse>> getWorkerJobs(
            @PathVariable String workerId,
            @RequestParam String workerSecret) {

        return ResponseEntity.ok(
                workerService.getWorkerJobHistory(workerId, workerSecret));
    }
}