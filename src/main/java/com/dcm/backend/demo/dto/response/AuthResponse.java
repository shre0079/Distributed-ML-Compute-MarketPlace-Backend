package com.dcm.backend.demo.dto.response;

import java.math.BigDecimal;

public class AuthResponse {
    public String token;
    public String userId;
    public String email;
    public BigDecimal walletBalance;

    public AuthResponse(String token, String userId, String email, BigDecimal walletBalance) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.walletBalance = walletBalance;
    }
}