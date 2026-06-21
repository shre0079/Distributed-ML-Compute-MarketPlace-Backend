package com.dcm.backend.demo.dto.entity;

import com.dcm.backend.demo.enums.WithdrawalStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {

    @Id
    public String withdrawalId;

    @Column(nullable = false)
    public String workerId;

    @Column(precision = 12, scale = 8, nullable = false)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(nullable = false)
    public long requestedAt;

    public long resolvedAt;     // 0 until approved/rejected

    public String adminNote;    // optional reason for rejection

    public WithdrawalRequest() {}

    public WithdrawalRequest(String withdrawalId, String workerId, BigDecimal amount) {
        this.withdrawalId = withdrawalId;
        this.workerId = workerId;
        this.amount = amount;
        this.status = WithdrawalStatus.PENDING;
        this.requestedAt = System.currentTimeMillis();
    }
}