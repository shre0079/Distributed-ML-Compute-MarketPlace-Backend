package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.Job;
import com.dcm.backend.demo.dto.Transaction;
import com.dcm.backend.demo.dto.User;
import com.dcm.backend.demo.dto.WorkerInfo;
import com.dcm.backend.demo.repository.JobRepository;
import com.dcm.backend.demo.repository.TransactionRepository;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.repository.WorkerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WalletService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JobRepository jobRepo;

    @Autowired
    private WorkerRepository workerRepo;

    @Autowired
    private TransactionRepository transactionRepo;

    private void recordUserTransaction(User user, Job job) {

        Transaction txn = new Transaction();

        txn.transactionId = UUID.randomUUID().toString();
        txn.userId = user.userId;
        txn.jobId = job.jobId;
        txn.type = "JOB_COST";
        txn.amount = job.cost.negate();
        txn.timestamp = System.currentTimeMillis();

        transactionRepo.save(txn);
    }

    private void recordWorkerTransaction(WorkerInfo worker, Job job) {

        Transaction txn = new Transaction();

        txn.transactionId = UUID.randomUUID().toString();
        txn.workerId = worker.workerId;
        txn.jobId = job.jobId;
        txn.type = "WORKER_PAYOUT";
        txn.amount = job.workerReward;
        txn.timestamp = System.currentTimeMillis();

        transactionRepo.save(txn);
    }

    @Transactional
    public void processJobPayment(String jobId) {

        Job job =  jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.userId == null) {
            throw new RuntimeException("Job has no userId: " + job.jobId);
        }

        User user = userRepo.findById(job.userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WorkerInfo worker = workerRepo.findById(job.workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        user.walletBalance = user.walletBalance.subtract(job.cost);

        worker.walletBalance = worker.walletBalance.add(job.workerReward);
        worker.totalEarned = worker.totalEarned.add(job.workerReward);

        userRepo.save(user);
        workerRepo.save(worker);

        recordUserTransaction(user, job);
        recordWorkerTransaction(worker, job);

        System.out.println("Processing wallet payment for job: " + job.jobId);

        System.out.println(job);
        System.out.println("userId = " + job.userId);
    }
}
