package com.sacco.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Member number is required")
    private String memberNumber;

    @NotBlank(message = "Password is required")
    private String password;
}