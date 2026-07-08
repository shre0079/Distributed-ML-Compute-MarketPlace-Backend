package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.*;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.enums.WithdrawalStatus;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final WithdrawalRepository withdrawalRepository;

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);


    public AdminService(JobRepository jobRepository,
                        WorkerRepository workerRepository,
                        UserRepository userRepository,
                        TransactionRepository transactionRepository,
                        WalletService walletService, WithdrawalRepository withdrawalRepository) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.withdrawalRepository = withdrawalRepository;
    }

    private void recordAudit(String adminUserId, String action, String targetId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.auditId = UUID.randomUUID().toString();
        log.adminUserId = adminUserId;
        log.action = action;
        log.targetId = targetId;
        log.details = details;
        log.timestamp = System.currentTimeMillis();
        auditLogRepository.save(log);
    }

    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    public Page<Job> getJobsByStatus(JobStatus status, Pageable pageable) {
        return jobRepository.findAllByStatus(status, pageable);
    }

    public Page<WorkerInfo> getAllWorkers(Pageable pageable) {
        return workerRepository.findAll(pageable);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    // Force fail a stuck job
    @Transactional
    public Job forceFailJob(String jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        if (job.status == JobStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Job is already FAILED");
        }

        if (job.status == JobStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "Cannot force fail a completed job");
        }

        job.status = JobStatus.FAILED;
        jobRepository.save(job);

        // Refund user if estimate was pre-deducted
        if (job.estimatedCost != null &&
                job.estimatedCost.compareTo(BigDecimal.ZERO) > 0) {
            walletService.processFailureRefund(jobId);
        }

        // forceFailJob
        log.warn("Admin force-failed job: {}", jobId);

        return job;
    }

    // Ban a worker — marks them inactive
    @Transactional
    public WorkerInfo banWorker(String workerId) {

        WorkerInfo worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        // Reset all their running jobs
        List<Job> runningJobs = jobRepository
                .findAllByWorkerIdAndStatus(workerId, JobStatus.RUNNING);

        for (Job job : runningJobs) {
            job.status = JobStatus.CREATED;
            job.workerId = null;
            jobRepository.save(job);
            // banWorker
            log.warn("Requeued job {} from banned worker {}", job.jobId, workerId);
        }

        // Set lastSeen to 0 so worker appears permanently offline
        worker.lastSeen = 0;
        workerRepository.save(worker);

        log.warn("Admin banned worker: {}", workerId);
        return worker;
    }

    // Platform stats
    public Map<String, Object> getPlatformStats() {

        long now = System.currentTimeMillis();

        List<WorkerInfo> allWorkers = workerRepository.findAll();
        long onlineWorkers = allWorkers.stream()
                .filter(w -> (now - w.lastSeen) <= 15000)
                .count();

        long totalJobs = jobRepository.count();
        long runningJobs = jobRepository
                .findAllByStatus(JobStatus.RUNNING).size();
        long pendingJobs = jobRepository
                .findAllByStatus(JobStatus.CREATED).size();
        long successJobs = jobRepository
                .findAllByStatus(JobStatus.SUCCESS).size();
        long failedJobs = jobRepository
                .findAllByStatus(JobStatus.FAILED).size();

        // Total platform revenue (sum of all platformFee)
        BigDecimal totalRevenue = jobRepository.findAll()
                .stream()
                .filter(j -> j.platformFee != null)
                .map(j -> j.platformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total worker payouts
        BigDecimal totalWorkerPayouts = workerRepository.findAll()
                .stream()
                .map(w -> w.totalEarned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("workers", Map.of(
                "total", allWorkers.size(),
                "online", onlineWorkers,
                "offline", allWorkers.size() - onlineWorkers
        ));
        stats.put("jobs", Map.of(
                "total", totalJobs,
                "running", runningJobs,
                "pending", pendingJobs,
                "success", successJobs,
                "failed", failedJobs
        ));
        stats.put("financials", Map.of(
                "totalPlatformRevenue", totalRevenue,
                "totalWorkerPayouts", totalWorkerPayouts
        ));

        return stats;
    }

    public List<WithdrawalRequest> getPendingWithdrawals() {
        return withdrawalRepository.findAllByStatus(WithdrawalStatus.PENDING);
    }

//    public WithdrawalRequest approveWithdrawal(String withdrawalId) {
//        return walletService.approveWithdrawal(withdrawalId);
//    }
//
//    public WithdrawalRequest rejectWithdrawal(String withdrawalId, String reason) {
//        return walletService.rejectWithdrawal(withdrawalId, reason);
//    }

    public WithdrawalRequest approveWithdrawal(String withdrawalId, String adminUserId) {
        WithdrawalRequest w = walletService.approveWithdrawal(withdrawalId);
        recordAudit(adminUserId, "APPROVE_WITHDRAWAL", withdrawalId, null);
        return w;
    }

    public WithdrawalRequest rejectWithdrawal(String withdrawalId, String reason, String adminUserId) {
        WithdrawalRequest w = walletService.rejectWithdrawal(withdrawalId, reason);
        recordAudit(adminUserId, "REJECT_WITHDRAWAL", withdrawalId, reason);
        return w;
    }

    public Page<AdminAuditLog> getAuditLog(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}