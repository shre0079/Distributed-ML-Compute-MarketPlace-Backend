package com.dcm.backend.demo.dto.entity;

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

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String password; // stores BCrypt hash, never plaintext

    @Column(precision = 12, scale = 8)
    public BigDecimal walletBalance = BigDecimal.ZERO;
}