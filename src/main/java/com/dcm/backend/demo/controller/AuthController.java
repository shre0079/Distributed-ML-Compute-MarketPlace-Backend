package com.dcm.backend.demo.controller;


import com.dcm.backend.demo.exception.RateLimitException;
import com.dcm.backend.demo.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.dcm.backend.demo.dto.request.LoginRequest;
import com.dcm.backend.demo.dto.request.UserRegistrationRequest;
import com.dcm.backend.demo.dto.response.AuthResponse;
import com.dcm.backend.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//register, login
@RestController
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/user/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        if (!rateLimitService.resolveRegisterBucket(ip).tryConsume(1)) {
            throw new RateLimitException(
                    "Too many registration attempts. Try again in a minute.");
        }

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/user/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        if (!rateLimitService.resolveLoginBucket(ip).tryConsume(1)) {
            throw new RateLimitException(
                    "Too many login attempts. Try again in a minute.");
        }

        return ResponseEntity.ok(authService.login(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
