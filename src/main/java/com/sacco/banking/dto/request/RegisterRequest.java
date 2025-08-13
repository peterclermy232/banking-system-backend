package com.sacco.banking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    // Remove memberNumber - it will be auto-generated

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
    private String password;

    @NotBlank(message = "National ID is required")
    @Size(min = 6, max = 20, message = "National ID must be between 6 and 20 characters")
    private String nationalId;

    private String address;
    private String occupation;
    private LocalDate dateOfBirth;

    @DecimalMin(value = "0.0", inclusive = true, message = "Share capital must be positive")
    private BigDecimal shareCapital = BigDecimal.ZERO;

    @Min(value = 0, message = "Monthly income must be positive")
    private Integer monthlyIncome = 0;

    @Size(max = 20, message = "ID number must not exceed 20 characters")
    private String idNumber;
}