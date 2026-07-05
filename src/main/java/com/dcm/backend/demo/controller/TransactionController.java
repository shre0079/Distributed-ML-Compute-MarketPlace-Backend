package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.Transaction;
import com.dcm.backend.demo.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Full transaction history with summary
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(transactionService.getUserTransactionHistory(userId, page, size));
    }

    // Transactions for a specific job
    @GetMapping("/transactions/job/{jobId}")
    public ResponseEntity<List<Transaction>> getJobTransactions(
            @PathVariable String jobId) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return ResponseEntity.ok(
                transactionService.getJobTransactions(jobId, userId));
    }
}