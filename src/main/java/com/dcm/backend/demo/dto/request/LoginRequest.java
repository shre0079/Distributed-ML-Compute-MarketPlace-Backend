package com.dcm.backend.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    public String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Must be alphanumeric (letters and numbers only)")
    public String password;
}
