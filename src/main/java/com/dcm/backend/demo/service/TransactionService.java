package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.Transaction;
import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.TransactionRepository;
import com.dcm.backend.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> getUserTransactionHistory(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userId));

        List<Transaction> transactions = transactionRepository
                .findAllByUserIdOrderByTimestampDesc(userId);

        // Calculate summary
        BigDecimal totalDeposited = transactions.stream()
                .filter(t -> t.type.equals("DEPOSIT"))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = transactions.stream()
                .filter(t -> t.type.equals("JOB_COST"))
                .map(t -> t.amount.abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunded = transactions.stream()
                .filter(t -> t.type.equals("REFUND"))
                .map(t -> t.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("email", user.email);
        response.put("currentBalance", user.walletBalance);
        response.put("summary", Map.of(
                "totalDeposited", totalDeposited,
                "totalSpent", totalSpent,
                "totalRefunded", totalRefunded,
                "transactionCount", transactions.size()
        ));
        response.put("transactions", transactions.stream()
                .map(this::formatTransaction)
                .toList());

        return response;
    }

    public List<Transaction> getJobTransactions(String jobId, String userId) {

        List<Transaction> transactions = transactionRepository
                .findAllByJobId(jobId);

        // Filter to only show transactions belonging to this user
        return transactions.stream()
                .filter(t -> userId.equals(t.userId))
                .toList();
    }

    private Map<String, Object> formatTransaction(Transaction t) {

        Map<String, Object> formatted = new LinkedHashMap<>();
        formatted.put("transactionId", t.transactionId);
        formatted.put("type", t.type);
        formatted.put("amount", t.amount);
        formatted.put("jobId", t.jobId);

        // Human readable timestamp
        formatted.put("timestamp", new java.util.Date(t.timestamp).toString());

        // Human readable description
        formatted.put("description", describeTransaction(t));

        return formatted;
    }

    private String describeTransaction(Transaction t) {
        return switch (t.type) {
            case "DEPOSIT" -> "Wallet deposit of $" + t.amount;
            case "DEPOSIT_HOLD" -> "Funds held for job " + t.jobId;
            case "JOB_COST" -> "Charged for job " + t.jobId;
            case "REFUND" -> "Refund for job " + t.jobId;
            case "WORKER_PAYOUT" -> "Earnings for job " + t.jobId;
            default -> t.type;
        };
    }
}