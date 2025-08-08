package com.sacco.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Destination account number is required")
    @Size(min = 5, max = 50, message = "Account number must be between 5 and 50 characters")
    private String toAccountNumber;

    @Size(max = 50, message = "Source account number must not exceed 50 characters")
    private String fromAccountNumber; // Optional - for account-to-account deposits

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phoneNumber; // Optional - for M-Pesa deposits

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String reference;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;
}