package com.sacco.banking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private String memberName;
    private String memberNumber;
    private BigDecimal totalBalance;
    private List<AccountSummaryResponse> accountSummaries;
    private List<TransactionResponse> recentTransactions;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private Integer creditScore;
    private BigDecimal shareCapital;
}