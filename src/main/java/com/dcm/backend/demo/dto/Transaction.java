package com.dcm.backend.demo.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

    @Entity
    public class Transaction {

        @Id
        public String transactionId;

        public String userId;

        public String workerId;

        public String jobId;

        public String type; // DEPOSIT / JOB_COST / WORKER_PAYOUT

        @Column(precision = 12, scale = 8)
        public BigDecimal amount;

        public long timestamp;
    }
}
