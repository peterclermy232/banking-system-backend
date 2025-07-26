package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Loan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private String loanNumber;
    private String loanType;
    private BigDecimal principalAmount;
    private BigDecimal currentBalance;
    private BigDecimal monthlyPayment;
    private BigDecimal interestRate;
    private Integer termMonths;
    private Integer remainingMonths;
    private String status;
    private String purpose;
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime disbursementDate;
    private LocalDateTime nextPaymentDate;

    public static LoanResponse fromEntity(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .loanType(loan.getLoanType().name())
                .principalAmount(loan.getPrincipalAmount())
                .currentBalance(loan.getCurrentBalance())
                .monthlyPayment(loan.getMonthlyPayment())
                .interestRate(loan.getInterestRate())
                .termMonths(loan.getTermMonths())
                .remainingMonths(loan.getRemainingMonths())
                .status(loan.getStatus().name())
                .purpose(loan.getPurpose())
                .applicationDate(loan.getApplicationDate())
                .approvalDate(loan.getApprovalDate())
                .disbursementDate(loan.getDisbursementDate())
                .nextPaymentDate(loan.getNextPaymentDate())
                .build();
    }
}
