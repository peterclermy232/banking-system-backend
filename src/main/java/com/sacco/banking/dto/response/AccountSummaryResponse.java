package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Account;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountSummaryResponse {
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal minimumBalance;
    private BigDecimal interestRate;
    private String status;

    public static AccountSummaryResponse fromEntity(Account account) {
        return AccountSummaryResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .minimumBalance(account.getMinimumBalance())
                .interestRate(account.getInterestRate())
                .status(account.getStatus().name())
                .build();
    }
}