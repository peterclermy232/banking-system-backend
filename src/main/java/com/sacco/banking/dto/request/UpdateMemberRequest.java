package com.sacco.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateMemberRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String address;

    private LocalDate dateOfBirth;

    private String occupation;

    private BigDecimal shareCapital;

    private Integer monthlyIncome;
    private String idNumber;

    // Note: Sensitive fields like memberNumber, idNumber, status,
    // and financial balances are not included for security reasons
}