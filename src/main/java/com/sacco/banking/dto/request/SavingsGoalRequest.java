package com.sacco.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SavingsGoalRequest {
    @NotBlank(message = "Goal name is required")
    private String goalName;

    private String description;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "100.0", message = "Minimum target amount is 100")
    private BigDecimal targetAmount;

    @NotNull(message = "Target date is required")
    @Future(message = "Target date must be in the future")
    private LocalDateTime targetDate;
}