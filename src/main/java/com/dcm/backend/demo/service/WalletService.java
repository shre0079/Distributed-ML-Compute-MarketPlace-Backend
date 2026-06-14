package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Job;
import com.dcm.backend.demo.dto.entity.Transaction;
import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.dto.entity.WorkerInfo;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.TransactionRepository;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final UserRepository userRepo;
    private final JobRepository jobRepo;
    private final WorkerRepository workerRepo;
    private final TransactionRepository transactionRepo;

    public WalletService(UserRepository userRepo,
                         JobRepository jobRepo,
                         WorkerRepository workerRepo,
                         TransactionRepository transactionRepo) {
        this.userRepo = userRepo;
        this.jobRepo = jobRepo;
        this.workerRepo = workerRepo;
        this.transactionRepo = transactionRepo;
    }

    // Called at job creation — pre-deduct estimated cost
    @Transactional
    public void holdJobEstimate(Job job, User user) {

        // Deduct estimated cost upfront
        user.walletBalance = user.walletBalance.subtract(job.estimatedCost);
        userRepo.save(user);

        // Record hold transaction
        recordTransaction(user.userId, null, job.jobId,
                "DEPOSIT_HOLD", job.estimatedCost.negate());

        System.out.println("Pre-deducted $" + job.estimatedCost +
                " from user " + user.userId + " for job " + job.jobId);
    }

    // Called at job SUCCESS — charge actual, refund difference
    @Transactional
    public void processJobPayment(String jobId) {

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        User user = userRepo.findById(job.userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WorkerInfo worker = workerRepo.findById(job.workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));

        // Calculate refund (estimate already deducted at creation)
        BigDecimal refund = BillingService.calculateRefund(job);

        // Refund unused portion back to user
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            user.walletBalance = user.walletBalance.add(refund);
            userRepo.save(user);
            recordTransaction(user.userId, null, jobId, "REFUND", refund);
            System.out.println("Refunded $" + refund + " to user " + user.userId);
        }

        // Pay worker based on actual cost
        worker.walletBalance = worker.walletBalance.add(job.workerReward);
        worker.totalEarned = worker.totalEarned.add(job.workerReward);
        workerRepo.save(worker);

        // Record transactions
        recordTransaction(user.userId, null, jobId,
                "JOB_COST", job.cost.negate());
        recordTransaction(null, worker.workerId, jobId,
                "WORKER_PAYOUT", job.workerReward);

        System.out.println("Payment processed for job " + jobId +
                " | actual=$" + job.cost +
                " | workerEarned=$" + job.workerReward +
                " | refund=$" + refund);
    }

    // Called at job TIMEOUT — charge full estimate, pay worker full
    @Transactional
    public void processTimeoutPayment(String jobId) {

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        WorkerInfo worker = workerRepo.findById(job.workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        // No refund — user already pre-paid full estimate
        // Set cost = estimatedCost for record keeping
        job.cost = job.estimatedCost;
        job.workerReward = job.estimatedCost
                .multiply(BigDecimal.valueOf(0.7))
                .setScale(8, java.math.RoundingMode.HALF_UP);
        job.platformFee = job.estimatedCost
                .multiply(BigDecimal.valueOf(0.3))
                .setScale(8, java.math.RoundingMode.HALF_UP);
        jobRepo.save(job);

        // Pay worker full estimate reward
        worker.walletBalance = worker.walletBalance.add(job.workerReward);
        worker.totalEarned = worker.totalEarned.add(job.workerReward);
        workerRepo.save(worker);

        // Record transactions
        recordTransaction(job.userId, null, jobId,
                "JOB_COST", job.cost.negate());
        recordTransaction(null, worker.workerId, jobId,
                "WORKER_PAYOUT", job.workerReward);

        System.out.println("Timeout payment processed for job " + jobId +
                " | charged=$" + job.cost);
    }

    // Called at job FAILED — full refund to user
    @Transactional
    public void processFailureRefund(String jobId) {

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        User user = userRepo.findById(job.userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Full refund of pre-deducted estimate
        user.walletBalance = user.walletBalance.add(job.estimatedCost);
        userRepo.save(user);

        recordTransaction(user.userId, null, jobId,
                "REFUND", job.estimatedCost);

        System.out.println("Full refund of $" + job.estimatedCost +
                " to user " + user.userId + " for failed job " + jobId);
    }

    // Called at deposit — record transaction
    @Transactional
    public void recordDeposit(String userId, BigDecimal amount) {
        recordTransaction(userId, null, null, "DEPOSIT", amount);
    }

    // Called when user cancels a CREATED job — full refund
    @Transactional
    public void processJobCancellation(String jobId) {

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found: " + jobId));

        User user = userRepo.findById(job.userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + job.userId));

        // Full refund of pre-deducted estimate
        user.walletBalance = user.walletBalance.add(job.estimatedCost);
        userRepo.save(user);

        recordTransaction(user.userId, null, jobId,
                "REFUND", job.estimatedCost);

        System.out.println("Job " + jobId + " cancelled" +
                " | refunded=$" + job.estimatedCost +
                " to user " + user.userId);
    }

    private void recordTransaction(String userId, String workerId,
                                   String jobId, String type,
                                   BigDecimal amount) {
        Transaction txn = new Transaction();
        txn.transactionId = UUID.randomUUID().toString();
        txn.userId = userId;
        txn.workerId = workerId;
        txn.jobId = jobId;
        txn.type = type;
        txn.amount = amount;
        txn.timestamp = System.currentTimeMillis();
        transactionRepo.save(txn);
    }
}