package com.sacco.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanCalculationRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum amount is 1,000")
    private BigDecimal amount;

    @NotNull(message = "Term is required")
    @Min(value = 6, message = "Minimum term is 6 months")
    private Integer termMonths;

    @NotBlank(message = "Loan type is required")
    private String loanType;
}
