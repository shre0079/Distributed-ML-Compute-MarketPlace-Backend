package com.dcm.backend.demo.service;

import com.dcm.backend.demo.dto.entity.User;
import com.dcm.backend.demo.dto.request.LoginRequest;
import com.dcm.backend.demo.dto.request.UserRegistrationRequest;
import com.dcm.backend.demo.dto.response.AuthResponse;
import com.dcm.backend.demo.exception.ConflictException;
import com.dcm.backend.demo.exception.UnauthorizedException;
import com.dcm.backend.demo.repository.UserRepository;
import com.dcm.backend.demo.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(UserRegistrationRequest request) {

        // Check email already taken
        if (userRepository.findByEmail(request.email).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.userId = UUID.randomUUID().toString();
        user.email = request.email;
        user.password = passwordEncoder.encode(request.password); // BCrypt hash
        user.walletBalance = BigDecimal.ZERO;

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.userId, user.email);

        return new AuthResponse(token, user.userId, user.email, user.walletBalance);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.userId, user.email);

        return new AuthResponse(token, user.userId, user.email, user.walletBalance);
    }
}