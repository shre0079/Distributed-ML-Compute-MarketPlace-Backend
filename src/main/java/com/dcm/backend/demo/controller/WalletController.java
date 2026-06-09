package com.dcm.backend.demo.controller;

import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
public class WalletController {

    private final UserRepository userRepository;

    public WalletController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/deposit")
    public User deposit(@RequestParam BigDecimal amount) {

        // Extract userId from JWT
        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.walletBalance = user.walletBalance.add(amount);
        userRepository.save(user);
        return user;
    }

    @GetMapping("/wallet")
    public User getWallet() {

        // Extract userId from JWT
        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}