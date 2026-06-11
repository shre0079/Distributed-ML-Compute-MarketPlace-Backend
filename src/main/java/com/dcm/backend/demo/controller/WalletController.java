package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.exception.ResourceNotFoundException;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.service.WalletService;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Validated
public class WalletController {

    private final UserRepository userRepository;
    private final WalletService walletService;



    public WalletController(UserRepository userRepository, WalletService walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @PostMapping("/deposit")
    public User deposit(@RequestParam @DecimalMin(value = "0.01", message = "Deposit amount must be at least $0.01") BigDecimal amount) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.walletBalance = user.walletBalance.add(amount);
        userRepository.save(user);

        // Record deposit transaction
        walletService.recordDeposit(userId, amount);

        return user;
    }

    @GetMapping("/wallet")
    public User getWallet() {

        // Extract userId from JWT
        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}