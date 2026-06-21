package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    // Find all transactions for a user, newest first
    List<Transaction> findAllByUserIdOrderByTimestampDesc(String userId);

    // Find all transactions for a worker, newest first
    List<Transaction> findAllByWorkerIdOrderByTimestampDesc(String workerId);

    // Find all transactions for a specific job
    List<Transaction> findAllByJobId(String jobId);
}