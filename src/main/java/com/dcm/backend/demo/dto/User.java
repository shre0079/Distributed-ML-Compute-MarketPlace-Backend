package com.dcm.backend.demo.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    public String userId;

    public String email;

    @Column(precision = 12, scale = 8)
    public BigDecimal walletBalance = BigDecimal.ZERO;
}
