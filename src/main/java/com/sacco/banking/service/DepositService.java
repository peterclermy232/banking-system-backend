package com.sacco.banking.service;

import com.sacco.banking.dto.request.DepositRequest;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Transaction;
import com.sacco.banking.enums.TransactionStatus;
import com.sacco.banking.enums.TransactionType;
import com.sacco.banking.exception.InsufficientFundsException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse processCashDeposit(DepositRequest request, Member member) {
        log.info("Processing cash deposit for member {}", member.getMemberNumber());

        Account toAccount = getMemberAccount(member, request.getToAccountNumber());
        Transaction transaction = processDeposit(toAccount, request.getAmount(), TransactionType.DEPOSIT,
                request.getDescription() != null ? request.getDescription() : "Cash deposit",
                request.getReference());

        return mapToTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse processMpesaDeposit(DepositRequest request, Member member) {
        log.info("Processing M-Pesa deposit for member {}", member.getMemberNumber());

        Account toAccount = getMemberAccount(member, request.getToAccountNumber());
        Transaction transaction = processDeposit(toAccount, request.getAmount(), TransactionType.MPESA_DEPOSIT,
                request.getDescription() != null ? request.getDescription() : "M-Pesa deposit",
                request.getReference());

        return mapToTransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse processAccountToAccountDeposit(DepositRequest request, Member member) {
        log.info("Processing account-to-account deposit for member {}", member.getMemberNumber());

        Account fromAccount = getMemberAccount(member, request.getFromAccountNumber());
        Account toAccount = getMemberAccount(member, request.getToAccountNumber());

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = createTransaction(toAccount, request.getAmount(), TransactionType.TRANSFER,
                request.getDescription() != null ? request.getDescription() : "Account Transfer",
                request.getReference());

        transaction.setFromAccount(fromAccount);
        transactionRepository.save(transaction);

        return mapToTransactionResponse(transaction);
    }

    private Transaction processDeposit(Account toAccount, BigDecimal amount, TransactionType type, String description, String reference) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid deposit amount");
        }

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        Transaction transaction = createTransaction(toAccount, amount, type, description, reference);
        transactionRepository.save(transaction);

        return transaction;
    }

    private Account getMemberAccount(Member member, String accountNumber) {
        return accountRepository.findByAccountNumberAndMemberMemberNumber(accountNumber, member.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Account not found or does not belong to member"));
    }

    private Transaction createTransaction(Account toAccount, BigDecimal amount, TransactionType type, String description, String reference) {
        return Transaction.builder()
                .toAccount(toAccount)
                .amount(amount)
                .transactionType(type)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .reference(reference)
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .toAccountNumber(transaction.getToAccount() != null ? transaction.getToAccount().getAccountNumber() : null)
                .fromAccountNumber(transaction.getFromAccount() != null ? transaction.getFromAccount().getAccountNumber() : null)
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType().name())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .reference(transaction.getReference())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
