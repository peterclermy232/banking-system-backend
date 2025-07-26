package com.sacco.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotBlank(message = "Loan type is required")
    private String loanType; // PERSONAL, BUSINESS, EMERGENCY, ASSET_FINANCING, EDUCATION

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "5000.0", message = "Minimum loan amount is 5,000")
    private BigDecimal amount;

    @NotNull(message = "Term is required")
    @Min(value = 6, message = "Minimum term is 6 months")
    private Integer termMonths;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}