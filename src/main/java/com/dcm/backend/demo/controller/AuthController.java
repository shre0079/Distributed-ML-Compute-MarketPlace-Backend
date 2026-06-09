package com.dcm.backend.demo.controller;


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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserRegistrationRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
