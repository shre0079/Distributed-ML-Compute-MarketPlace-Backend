package com.dcm.backend.demo.repository;

import com.dcm.backend.demo.dto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {}