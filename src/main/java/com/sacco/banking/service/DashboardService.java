package com.sacco.banking.service;

import com.sacco.banking.dto.response.DashboardResponse;
import com.sacco.banking.dto.response.AccountSummaryResponse;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Transaction;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardResponse getDashboardData(Member member) {
        List<Account> accounts = accountRepository.findByMember(member);

        // Account summaries
        List<AccountSummaryResponse> accountSummaries = accounts.stream()
                .map(AccountSummaryResponse::fromEntity)
                .collect(Collectors.toList());

        // Recent transactions (last 10)
        List<TransactionResponse> recentTransactions = accounts.stream()
                .flatMap(account -> transactionRepository
                        .findTransactionsByAccount(account, PageRequest.of(0, 10))
                        .getContent().stream())
                .map(TransactionResponse::fromEntity)
                .limit(10)
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate monthly income and expenses
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<Transaction> monthlyTransactions = transactionRepository
                .findTransactionsByAccountsAndDateRange(accounts, monthStart, LocalDateTime.now());

        BigDecimal monthlyIncome = monthlyTransactions.stream()
                .filter(t -> accounts.contains(t.getToAccount()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyExpenses = monthlyTransactions.stream()
                .filter(t -> accounts.contains(t.getFromAccount()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.builder()
                .memberName(member.getFirstName() + " " + member.getLastName())
                .memberNumber(member.getMemberNumber())
                .totalBalance(totalBalance)
                .accountSummaries(accountSummaries)
                .recentTransactions(recentTransactions)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .creditScore(member.getCreditScore())
                .shareCapital(member.getShareCapital())
                .roles(
                        member.getRoles().stream()
                                .map(role -> role.getName().name()) // Converts enum to String
                                .toList()
                )
                .build();
    }
}