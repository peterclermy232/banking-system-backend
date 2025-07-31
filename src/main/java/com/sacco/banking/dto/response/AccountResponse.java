package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Account;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private BigDecimal minimumBalance;
    private BigDecimal interestRate;
    private Account.AccountStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime closedDate;
    private String memberNumber;
    private String memberName;

    public static AccountResponse fromEntity(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .minimumBalance(account.getMinimumBalance())
                .interestRate(account.getInterestRate())
                .status(account.getStatus())
                .createdDate(account.getCreatedDate())
                .updatedDate(account.getUpdatedDate())
                .closedDate(account.getClosedDate())
                .memberNumber(account.getMember().getMemberNumber())
                .memberName(account.getMember().getFirstName() + " " + account.getMember().getLastName())
                .build();
    }
}