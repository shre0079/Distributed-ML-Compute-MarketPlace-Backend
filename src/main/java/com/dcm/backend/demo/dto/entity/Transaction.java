package com.dcm.backend.demo.dto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    public String transactionId;
    @Column(nullable = false)
    public String userId;
    public String workerId;
    public String jobId;
    public String type;

    @Column(precision = 12, scale = 8)
    public BigDecimal amount;
    public long timestamp;
}