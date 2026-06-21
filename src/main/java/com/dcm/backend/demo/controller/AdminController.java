package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.*;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // All jobs
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(adminService.getAllJobs());
    }

    // Jobs by status
    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(
            @PathVariable JobStatus status) {
        return ResponseEntity.ok(adminService.getJobsByStatus(status));
    }

    // Force fail a job
    @PostMapping("/jobs/{jobId}/force-fail")
    public ResponseEntity<Job> forceFailJob(@PathVariable String jobId) {
        return ResponseEntity.ok(adminService.forceFailJob(jobId));
    }

    // All workers
    @GetMapping("/workers")
    public ResponseEntity<List<WorkerInfo>> getAllWorkers() {
        return ResponseEntity.ok(adminService.getAllWorkers());
    }

    // Ban a worker
    @PostMapping("/workers/{workerId}/ban")
    public ResponseEntity<WorkerInfo> banWorker(
            @PathVariable String workerId) {
        return ResponseEntity.ok(adminService.banWorker(workerId));
    }

    // All users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // Full transaction audit trail
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(adminService.getAllTransactions());
    }

    // Platform stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<List<WithdrawalRequest>> getPendingWithdrawals() {
        return ResponseEntity.ok(
                adminService.getPendingWithdrawals());
    }

    @PostMapping("/withdrawals/{withdrawalId}/approve")
    public ResponseEntity<WithdrawalRequest> approveWithdrawal(
            @PathVariable String withdrawalId) {
        return ResponseEntity.ok(
                adminService.approveWithdrawal(withdrawalId));
    }

    @PostMapping("/withdrawals/{withdrawalId}/reject")
    public ResponseEntity<WithdrawalRequest> rejectWithdrawal(
            @PathVariable String withdrawalId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(
                adminService.rejectWithdrawal(withdrawalId, reason));
    }
}