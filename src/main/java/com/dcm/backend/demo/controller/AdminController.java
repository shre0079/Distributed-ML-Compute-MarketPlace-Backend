package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.*;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.service.AdminService;
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
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/jobs")
    public Page<Job> getAllJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getAllJobs(pageable);
    }

    @GetMapping("/jobs/status/{status}")
    public Page<Job> getJobsByStatus(
            @PathVariable JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getJobsByStatus(status, pageable);
    }

    @GetMapping("/workers")
    public Page<WorkerInfo> getAllWorkers(@PageableDefault(size = 20) Pageable pageable) {
        return adminService.getAllWorkers(pageable);
    }

    @GetMapping("/users")
    public Page<User> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @GetMapping("/transactions")
    public Page<Transaction> getAllTransactions(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getAllTransactions(pageable);
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

//    @PostMapping("/withdrawals/{withdrawalId}/approve")
//    public ResponseEntity<WithdrawalRequest> approveWithdrawal(
//            @PathVariable String withdrawalId) {
//        return ResponseEntity.ok(
//                adminService.approveWithdrawal(withdrawalId));
//    }
//
//    @PostMapping("/withdrawals/{withdrawalId}/reject")
//    public ResponseEntity<WithdrawalRequest> rejectWithdrawal(
//            @PathVariable String withdrawalId,
//            @RequestParam(required = false) String reason) {
//        return ResponseEntity.ok(
//                adminService.rejectWithdrawal(withdrawalId, reason));
//    }

    @PostMapping("/jobs/{jobId}/force-fail")
    public ResponseEntity<Job> forceFailJob(@PathVariable String jobId) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.forceFailJob(jobId, adminUserId));
    }

    @PostMapping("/workers/{workerId}/ban")
    public ResponseEntity<WorkerInfo> banWorker(@PathVariable String workerId) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.banWorker(workerId, adminUserId));
    }

    @PostMapping("/withdrawals/{withdrawalId}/approve")
    public ResponseEntity<WithdrawalRequest> approveWithdrawal(@PathVariable String withdrawalId) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.approveWithdrawal(withdrawalId, adminUserId));
    }

    @PostMapping("/withdrawals/{withdrawalId}/reject")
    public ResponseEntity<WithdrawalRequest> rejectWithdrawal(
            @PathVariable String withdrawalId,
            @RequestParam(required = false) String reason) {
        String adminUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(adminService.rejectWithdrawal(withdrawalId, reason, adminUserId));
    }

    @GetMapping("/audit-log")
    public Page<AdminAuditLog> getAuditLog(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminService.getAuditLog(pageable);
    }
}