package com.sacco.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username/Email is required")
    private String username; // Can be memberNumber or email

    @NotBlank(message = "Password is required")
    private String password;
}