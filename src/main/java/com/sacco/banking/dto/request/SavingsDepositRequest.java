package com.sacco.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SavingsDepositRequest {
    @NotBlank(message = "From account is required")
    private String fromAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10.0", message = "Minimum deposit amount is 10")
    private BigDecimal amount;

    private String reference;
    private Long savingsGoalId; // Optional: link to specific savings goal
}