package com.dcm.backend.demo.dto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    public String transactionId;
    public String userId;
    public String workerId;
    public String jobId;
    public String type;
    public BigDecimal amount;
    public long timestamp;
}