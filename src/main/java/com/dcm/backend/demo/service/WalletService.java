package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.*;
import com.dcm.backend.demo.enums.WithdrawalStatus;
import com.dcm.backend.demo.exception.InsufficientBalanceException;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final UserRepository userRepo;
    private final JobRepository jobRepo;
    private final WorkerRepository workerRepo;
    private final TransactionRepository transactionRepo;
    private final WithdrawalRepository withdrawalRepo;

    public WalletService(UserRepository userRepo,
                         JobRepository jobRepo,
                         WorkerRepository workerRepo,
                         TransactionRepository transactionRepo, WithdrawalRepository withdrawalRepo) {
        this.userRepo = userRepo;
        this.jobRepo = jobRepo;
        this.workerRepo = workerRepo;
        this.transactionRepo = transactionRepo;
        this.withdrawalRepo = withdrawalRepo;
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

        // holdJobEstimate
        log.info("Pre-deducted ${} from user {} for job {}", job.estimatedCost, user.userId, job.jobId);

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
            log.info("Refunded ${} to user {}", refund, user.userId);
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

        // processJobPayment — final summary
        log.info("Payment processed for job {} | actual=${} | workerEarned=${} | refund=${}",
                jobId, job.cost, job.workerReward, refund);
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

        log.info("Timeout payment processed for job {} | charged=${}", jobId, job.cost);
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

        log.info("Full refund of ${} to user {} for failed job {}", job.estimatedCost, user.userId, jobId);

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

        log.info("Job {} cancelled | refunded=${} to user {}", jobId, job.estimatedCost, user.userId);
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

    // Worker requests withdrawal — deduct immediately, mark PENDING
    @Transactional
    public WithdrawalRequest requestWithdrawal(String workerId, BigDecimal amount) {

        WorkerInfo worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + workerId));

        if (worker.walletBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: $" + worker.walletBalance);
        }

        // Deduct immediately — funds are held pending approval
        worker.walletBalance = worker.walletBalance.subtract(amount);
        workerRepo.save(worker);

        WithdrawalRequest withdrawal = new WithdrawalRequest(
                UUID.randomUUID().toString(), workerId, amount);
        withdrawalRepo.save(withdrawal);

        recordTransaction(null, workerId, null,
                "WITHDRAWAL_HOLD", amount.negate());

        log.info("Withdrawal requested: worker={} amount=${}", workerId, amount);

        return withdrawal;
    }

    // Admin approves — funds officially leave the platform
    @Transactional
    public WithdrawalRequest approveWithdrawal(String withdrawalId) {

        WithdrawalRequest withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Withdrawal not found: " + withdrawalId));

        if (withdrawal.status != WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Withdrawal already resolved with status: " + withdrawal.status);
        }

        withdrawal.status = WithdrawalStatus.APPROVED;
        withdrawal.resolvedAt = System.currentTimeMillis();
        withdrawalRepo.save(withdrawal);

        recordTransaction(null, withdrawal.workerId, null,
                "WITHDRAWAL_APPROVED", withdrawal.amount.negate());

        log.info("Withdrawal approved: {} | worker={} | amount=${}", withdrawalId, withdrawal.workerId, withdrawal.amount);

        return withdrawal;
    }

    // Admin rejects — refund the held amount back to worker
    @Transactional
    public WithdrawalRequest rejectWithdrawal(String withdrawalId, String reason) {

        WithdrawalRequest withdrawal = withdrawalRepo.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Withdrawal not found: " + withdrawalId));

        if (withdrawal.status != WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Withdrawal already resolved with status: " + withdrawal.status);
        }

        WorkerInfo worker = workerRepo.findById(withdrawal.workerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Worker not found: " + withdrawal.workerId));

        // Refund the held amount
        worker.walletBalance = worker.walletBalance.add(withdrawal.amount);
        workerRepo.save(worker);

        withdrawal.status = WithdrawalStatus.REJECTED;
        withdrawal.resolvedAt = System.currentTimeMillis();
        withdrawal.adminNote = reason;
        withdrawalRepo.save(withdrawal);

        recordTransaction(null, withdrawal.workerId, null,
                "WITHDRAWAL_REJECTED", withdrawal.amount);

        // rejectWithdrawal
        log.info("Withdrawal rejected: {} | refunded=${}", withdrawalId, withdrawal.amount);

        return withdrawal;
    }

    @Transactional
    public void processJobExpiry(String jobId) {

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        User user = userRepo.findById(job.userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + job.userId));

        user.walletBalance = user.walletBalance.add(job.estimatedCost);
        userRepo.save(user);

        recordTransaction(user.userId, null, jobId, "REFUND", job.estimatedCost);

        System.out.println("Job " + jobId + " expired (worker never picked it up)" +
                " | refunded=$" + job.estimatedCost);
    }
}