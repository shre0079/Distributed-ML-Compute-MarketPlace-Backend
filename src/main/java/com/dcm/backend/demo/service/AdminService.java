package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.*;
import com.dcm.backend.demo.enums.JobStatus;
import com.dcm.backend.demo.enums.WithdrawalStatus;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.*;
import jakarta.transaction.Transactional;
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

    // All jobs across all users
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // Jobs filtered by status
    public List<Job> getJobsByStatus(JobStatus status) {
        return jobRepository.findAllByStatus(status);
    }

    // All workers with full details
    public List<WorkerInfo> getAllWorkers() {
        return workerRepository.findAll();
    }

    // All users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Full transaction audit trail
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
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

        System.out.println("Admin force-failed job: " + jobId);
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
            System.out.println("Requeued job " + job.jobId +
                    " from banned worker " + workerId);
        }

        // Set lastSeen to 0 so worker appears permanently offline
        worker.lastSeen = 0;
        workerRepository.save(worker);

        System.out.println("Admin banned worker: " + workerId);
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

    public WithdrawalRequest approveWithdrawal(String withdrawalId) {
        return walletService.approveWithdrawal(withdrawalId);
    }

    public WithdrawalRequest rejectWithdrawal(String withdrawalId, String reason) {
        return walletService.rejectWithdrawal(withdrawalId, reason);
    }
}