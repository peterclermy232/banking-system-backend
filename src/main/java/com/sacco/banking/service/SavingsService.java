package com.sacco.banking.service;

import com.sacco.banking.dto.request.SavingsGoalRequest;
import com.sacco.banking.dto.request.SavingsDepositRequest;
import com.sacco.banking.dto.response.SavingsGoalResponse;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.*;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.exception.InsufficientFundsException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.SavingsGoalRepository;
import com.sacco.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<SavingsGoalResponse> getMemberSavingsGoals(Member member) {
        List<SavingsGoal> goals = savingsGoalRepository.findByMemberOrderByCreatedAtDesc(member);
        return goals.stream()
                .map(SavingsGoalResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SavingsGoalResponse createSavingsGoal(SavingsGoalRequest request, Member member) {
        SavingsGoal goal = new SavingsGoal();
        goal.setGoalName(request.getGoalName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setTargetDate(request.getTargetDate());
        goal.setMember(member);
        goal.setStatus(SavingsGoal.GoalStatus.ACTIVE);

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return SavingsGoalResponse.fromEntity(savedGoal);
    }

    @Transactional
    public TransactionResponse makeSavingsDeposit(SavingsDepositRequest request, Member member) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new BadRequestException("Source account not found"));

        // Use the method that returns Optional<Account>
        Account savingsAccount = accountRepository.findFirstByMemberAndAccountType(member, Account.AccountType.SAVINGS)
                .orElseThrow(() -> new BadRequestException("Savings account not found"));

        // Verify ownership
        if (!fromAccount.getMember().getId().equals(member.getId())) {
            throw new BadRequestException("You don't own the source account");
        }

        // Check balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(Transaction.TransactionType.SAVINGS_DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setFee(BigDecimal.ZERO);
        transaction.setDescription("Savings deposit");
        transaction.setReference(request.getReference());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(savingsAccount);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        savingsAccount.setBalance(savingsAccount.getBalance().add(request.getAmount()));

        // Update savings goal if specified
        if (request.getSavingsGoalId() != null) {
            SavingsGoal goal = savingsGoalRepository.findByIdAndMember(request.getSavingsGoalId(), member)
                    .orElseThrow(() -> new BadRequestException("Savings goal not found"));

            goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));

            // Check if goal is completed
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                goal.setStatus(SavingsGoal.GoalStatus.COMPLETED);
            }

            savingsGoalRepository.save(goal);
        }

        // Save changes
        accountRepository.save(fromAccount);
        accountRepository.save(savingsAccount);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional
    public SavingsGoalResponse updateSavingsGoal(Long goalId, SavingsGoalRequest request, Member member) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndMember(goalId, member)
                .orElseThrow(() -> new BadRequestException("Savings goal not found"));

        goal.setGoalName(request.getGoalName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);
        return SavingsGoalResponse.fromEntity(savedGoal);
    }

    private String generateTransactionId() {
        return "SAV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}