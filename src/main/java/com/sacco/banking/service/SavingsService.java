package com.sacco.banking.service;

import com.sacco.banking.dto.request.SavingsGoalRequest;
import com.sacco.banking.dto.request.SavingsDepositRequest;
import com.sacco.banking.dto.response.SavingsGoalResponse;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.*;
import com.sacco.banking.enums.AccountType;
import com.sacco.banking.enums.NotificationType;
import com.sacco.banking.enums.TransactionStatus;
import com.sacco.banking.enums.TransactionType;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.exception.InsufficientFundsException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.SavingsGoalRepository;
import com.sacco.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationHelper notificationHelper; // Add notification helper

    @Transactional(readOnly = true)
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
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);

        // Send notification for goal creation
        String message = String.format("Your savings goal '%s' for KSH %.2f has been created successfully. Target date: %s",
                goal.getGoalName(),
                goal.getTargetAmount().doubleValue(),
                goal.getTargetDate().toString());

        notificationHelper.notifySystemNotification(
                member.getMemberNumber(),
                "Savings Goal Created",
                message,
                NotificationType.SUCCESS,
                1
        );

        log.info("Created savings goal '{}' for member {}", goal.getGoalName(), member.getMemberNumber());

        return SavingsGoalResponse.fromEntity(savedGoal);
    }

    @Transactional
    public TransactionResponse makeSavingsDeposit(SavingsDepositRequest request, Member member) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new BadRequestException("Source account not found"));

        // Use the method that returns Optional<Account>
        Account savingsAccount = accountRepository.findFirstByMemberAndAccountType(member, AccountType.SAVINGS)
                .orElseThrow(() -> new BadRequestException("Savings account not found"));

        // Verify ownership
        if (!fromAccount.getMember().getId().equals(member.getId())) {
            throw new BadRequestException("You don't own the source account");
        }

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Deposit amount must be greater than zero");
        }

        // Check balance (considering minimum balance)
        BigDecimal availableBalance = fromAccount.getBalance().subtract(fromAccount.getMinimumBalance());
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available balance: " + availableBalance);
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(TransactionType.SAVINGS_DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setFee(BigDecimal.ZERO);
        transaction.setDescription("Savings deposit" + (request.getReference() != null ? " - " + request.getReference() : ""));
        transaction.setReference(request.getReference());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(savingsAccount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        fromAccount.setUpdatedDate(LocalDateTime.now());

        savingsAccount.setBalance(savingsAccount.getBalance().add(request.getAmount()));
        savingsAccount.setUpdatedDate(LocalDateTime.now());

        SavingsGoal updatedGoal = null;
        boolean goalCompleted = false;

        // Update savings goal if specified
        if (request.getSavingsGoalId() != null) {
            SavingsGoal goal = savingsGoalRepository.findByIdAndMember(request.getSavingsGoalId(), member)
                    .orElseThrow(() -> new BadRequestException("Savings goal not found"));

            BigDecimal previousAmount = goal.getCurrentAmount();
            goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
            goal.setUpdatedAt(LocalDateTime.now());

            // Check if goal is completed
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0 &&
                    goal.getStatus() != SavingsGoal.GoalStatus.COMPLETED) {
                goal.setStatus(SavingsGoal.GoalStatus.COMPLETED);
                goal.setCompletedAt(LocalDateTime.now());
                goalCompleted = true;
            }

            updatedGoal = savingsGoalRepository.save(goal);

            // Send goal progress notification
            BigDecimal progressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (goalCompleted) {
                String message = String.format("Congratulations! You've completed your savings goal '%s'. You've saved KSH %.2f!",
                        goal.getGoalName(), goal.getCurrentAmount().doubleValue());

                notificationHelper.notifySystemNotification(
                        member.getMemberNumber(),
                        "Savings Goal Completed! 🎉",
                        message,
                        NotificationType.SUCCESS,
                        2
                );
            } else if (progressPercentage.compareTo(new BigDecimal("50")) >= 0 &&
                    previousAmount.divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal("100")).compareTo(new BigDecimal("50")) < 0) {
                // Halfway milestone
                String message = String.format("Great progress! You're halfway to your savings goal '%s'. Current: KSH %.2f / Target: KSH %.2f",
                        goal.getGoalName(), goal.getCurrentAmount().doubleValue(), goal.getTargetAmount().doubleValue());

                notificationHelper.notifySystemNotification(
                        member.getMemberNumber(),
                        "Savings Milestone Reached!",
                        message,
                        NotificationType.SUCCESS,
                        1
                );
            } else if (progressPercentage.compareTo(new BigDecimal("75")) >= 0 &&
                    previousAmount.divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal("100")).compareTo(new BigDecimal("75")) < 0) {
                // 75% milestone
                String message = String.format("Almost there! You're 75%% towards your savings goal '%s'. Current: KSH %.2f / Target: KSH %.2f",
                        goal.getGoalName(), goal.getCurrentAmount().doubleValue(), goal.getTargetAmount().doubleValue());

                notificationHelper.notifySystemNotification(
                        member.getMemberNumber(),
                        "Savings Milestone Reached!",
                        message,
                        NotificationType.SUCCESS,
                        1
                );
            }
        }

        // Save changes
        accountRepository.save(fromAccount);
        accountRepository.save(savingsAccount);
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Send deposit notification
        String depositMessage = String.format("You've successfully deposited KSH %.2f to your savings account.",
                request.getAmount().doubleValue());
        if (updatedGoal != null) {
            depositMessage += String.format(" This contribution was added to your '%s' savings goal.", updatedGoal.getGoalName());
        }

        notificationHelper.notifySystemNotification(
                member.getMemberNumber(),
                "Savings Deposit Successful",
                depositMessage,
                NotificationType.SUCCESS,
                1
        );

        // Check for low balance warning on source account
        if (fromAccount.getBalance().compareTo(fromAccount.getMinimumBalance().multiply(new BigDecimal("1.1"))) <= 0) {
            notificationHelper.notifyLowBalance(
                    member.getMemberNumber(),
                    fromAccount.getAccountType().name(),
                    fromAccount.getBalance().doubleValue()
            );
        }

        log.info("Savings deposit of {} completed for member {} from account {} to savings account",
                request.getAmount(), member.getMemberNumber(), fromAccount.getAccountNumber());

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional
    public SavingsGoalResponse updateSavingsGoal(Long goalId, SavingsGoalRequest request, Member member) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndMember(goalId, member)
                .orElseThrow(() -> new BadRequestException("Savings goal not found"));

        // Store old values for comparison
        String oldGoalName = goal.getGoalName();
        BigDecimal oldTargetAmount = goal.getTargetAmount();

        goal.setGoalName(request.getGoalName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setUpdatedAt(LocalDateTime.now());

        // Recalculate status if target amount changed
        if (!oldTargetAmount.equals(request.getTargetAmount())) {
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                goal.setStatus(SavingsGoal.GoalStatus.COMPLETED);
                if (goal.getCompletedAt() == null) {
                    goal.setCompletedAt(LocalDateTime.now());
                }
            } else if (goal.getStatus() == SavingsGoal.GoalStatus.COMPLETED) {
                goal.setStatus(SavingsGoal.GoalStatus.ACTIVE);
                goal.setCompletedAt(null);
            }
        }

        SavingsGoal savedGoal = savingsGoalRepository.save(goal);

        // Send update notification
        String changes = "";
        if (!oldGoalName.equals(request.getGoalName())) {
            changes += "name, ";
        }
        if (!oldTargetAmount.equals(request.getTargetAmount())) {
            changes += "target amount, ";
        }
        changes = changes.replaceAll(", $", ""); // Remove trailing comma

        String message = String.format("Your savings goal '%s' has been updated (%s). New target: KSH %.2f",
                goal.getGoalName(), changes, goal.getTargetAmount().doubleValue());

        notificationHelper.notifySystemNotification(
                member.getMemberNumber(),
                "Savings Goal Updated",
                message,
                NotificationType.INFO,
                1
        );

        log.info("Updated savings goal '{}' for member {}", goal.getGoalName(), member.getMemberNumber());

        return SavingsGoalResponse.fromEntity(savedGoal);
    }

    @Transactional
    public void deleteSavingsGoal(Long goalId, Member member) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndMember(goalId, member)
                .orElseThrow(() -> new BadRequestException("Savings goal not found"));

        String goalName = goal.getGoalName();
        savingsGoalRepository.delete(goal);

        // Send deletion notification
        String message = String.format("Your savings goal '%s' has been deleted.", goalName);
        notificationHelper.notifySystemNotification(
                member.getMemberNumber(),
                "Savings Goal Deleted",
                message,
                NotificationType.INFO,
                1
        );

        log.info("Deleted savings goal '{}' for member {}", goalName, member.getMemberNumber());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getSavingsGoal(Long goalId, Member member) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndMember(goalId, member)
                .orElseThrow(() -> new BadRequestException("Savings goal not found"));

        return SavingsGoalResponse.fromEntity(goal);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSavingsAmount(Member member) {
        Account savingsAccount = accountRepository.findFirstByMemberAndAccountType(member, AccountType.SAVINGS)
                .orElse(null);

        return savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalGoalAmount(Member member) {
        List<SavingsGoal> activeGoals = savingsGoalRepository.findByMemberAndStatus(member, SavingsGoal.GoalStatus.ACTIVE);
        return activeGoals.stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Method to check for goal deadlines and send reminders
     * Can be called by a scheduled task
     */
    @Transactional
    public void checkGoalDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekFromNow = now.plusWeeks(1);
        LocalDateTime oneMonthFromNow = now.plusMonths(1);

        // Find goals approaching deadline
        List<SavingsGoal> approachingGoals = savingsGoalRepository.findByStatusAndTargetDateBetween(
                SavingsGoal.GoalStatus.ACTIVE, now, oneMonthFromNow);

        for (SavingsGoal goal : approachingGoals) {
            BigDecimal remainingAmount = goal.getTargetAmount().subtract(goal.getCurrentAmount());

            if (goal.getTargetDate().isBefore(oneWeekFromNow) && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                // One week reminder
                String message = String.format("Your savings goal '%s' is due in less than a week! You need KSH %.2f more to reach your target.",
                        goal.getGoalName(), remainingAmount.doubleValue());

                notificationHelper.notifySystemNotification(
                        goal.getMember().getMemberNumber(),
                        "Savings Goal Deadline Approaching",
                        message,
                        NotificationType.WARNING,
                        2
                );
            } else if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                // One month reminder
                String message = String.format("Your savings goal '%s' is due in less than a month. You need KSH %.2f more to reach your target.",
                        goal.getGoalName(), remainingAmount.doubleValue());

                notificationHelper.notifySystemNotification(
                        goal.getMember().getMemberNumber(),
                        "Savings Goal Reminder",
                        message,
                        NotificationType.INFO,
                        1
                );
            }
        }

        log.info("Checked {} savings goals for deadline reminders", approachingGoals.size());
    }

    private String generateTransactionId() {
        return "SAV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}