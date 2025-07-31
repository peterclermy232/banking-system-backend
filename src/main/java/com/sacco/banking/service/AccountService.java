package com.sacco.banking.service;

import com.sacco.banking.dto.request.CreateAccountRequest;
import com.sacco.banking.dto.response.AccountResponse;
import com.sacco.banking.dto.response.AccountSummaryResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Notification;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final NotificationHelper notificationHelper;

    @Transactional
    public AccountResponse createAccount(String memberNumber, CreateAccountRequest request) {
        // Validate member exists
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        // Check if member already has this type of account
        boolean accountExists = accountRepository.existsByMemberMemberNumberAndAccountType(
                memberNumber, request.getAccountType());

        if (accountExists) {
            throw new BadRequestException("Member already has a " + request.getAccountType() + " account");
        }

        // Set account defaults based on type
        BigDecimal minimumBalance = getMinimumBalanceForAccountType(request.getAccountType());
        BigDecimal interestRate = getInterestRateForAccountType(request.getAccountType());

        // Create account
        Account account = Account.builder()
                .accountNumber(generateAccountNumber(request.getAccountType()))
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .minimumBalance(minimumBalance)
                .interestRate(interestRate)
                .member(member)
                .status(Account.AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        account = accountRepository.save(account);

        // Send notification
        notificationHelper.notifyAccountCreated(memberNumber, request.getAccountType().name());

        log.info("Created {} account {} for member {}",
                request.getAccountType(), account.getAccountNumber(), memberNumber);

        return AccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(String memberNumber) {
        List<Account> accounts = accountRepository.findByMemberMemberNumberAndStatusOrderByCreatedDateDesc(
                memberNumber, Account.AccountStatus.ACTIVE);

        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
//
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String memberNumber, Long accountId) {
        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));

        return AccountResponse.fromEntity(account);
    }

//    @Transactional(readOnly = true)
//    public AccountResponse getAccountByNumber(String memberNumber, String accountNumber) {
//        Account account = accountRepository.findByAccountNumberAndMemberMemberNumber(accountNumber, memberNumber)
//                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));
//
//        return AccountResponse.fromEntity(account);
//    }

    @Transactional
    public AccountResponse updateAccountStatus(String memberNumber, Long accountId, Account.AccountStatus status, String reason) {
        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));

        Account.AccountStatus oldStatus = account.getStatus();
        account.setStatus(status);
        account.setUpdatedDate(LocalDateTime.now());
        account = accountRepository.save(account);

        // Send appropriate notification based on status change
        sendStatusChangeNotification(memberNumber, account, oldStatus, status, reason);

        log.info("Updated account {} status from {} to {} for member {}. Reason: {}",
                account.getAccountNumber(), oldStatus, status, memberNumber, reason);

        return AccountResponse.fromEntity(account);
    }

    @Transactional
    public void closeAccount(String memberNumber, Long accountId, String reason) {
        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));

        // Check if account has balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot close account with positive balance. Please withdraw all funds first.");
        }

        account.setStatus(Account.AccountStatus.CLOSED);
        account.setClosedDate(LocalDateTime.now());
        account.setUpdatedDate(LocalDateTime.now());
        accountRepository.save(account);

        // Send notification
        String message = String.format("Your %s account (%s) has been closed. Reason: %s",
                account.getAccountType().name(), account.getAccountNumber(), reason);
        notificationHelper.notifyAccountNotification(
                memberNumber,
                account.getAccountType().name(),
                message,
                Notification.NotificationType.INFO
        );

        log.info("Closed account {} for member {}. Reason: {}",
                account.getAccountNumber(), memberNumber, reason);
    }

    @Transactional
    public void freezeAccount(String memberNumber, Long accountId, String reason) {
        updateAccountStatus(memberNumber, accountId, Account.AccountStatus.FROZEN, reason);
    }

    @Transactional
    public void unfreezeAccount(String memberNumber, Long accountId) {
        updateAccountStatus(memberNumber, accountId, Account.AccountStatus.ACTIVE, "Account unfrozen by member request");
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(String memberNumber) {
        List<Account> accounts = accountRepository.findByMemberMemberNumberAndStatus(
                memberNumber, Account.AccountStatus.ACTIVE);

        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accounts = accountRepository.findAllByOrderByCreatedDateDesc(pageable);
        return accounts.map(AccountResponse::fromEntity);
    }

    /**
     * Scheduled method to check for low balances - runs daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void checkLowBalances() {
        List<Account> activeAccounts = accountRepository.findByStatus(Account.AccountStatus.ACTIVE);

        for (Account account : activeAccounts) {
            // Check if balance is close to minimum balance (within 10% buffer)
            BigDecimal threshold = account.getMinimumBalance().multiply(new BigDecimal("1.1"));

            if (account.getBalance().compareTo(threshold) <= 0 &&
                    account.getBalance().compareTo(account.getMinimumBalance()) > 0) {

                notificationHelper.notifyLowBalance(
                        account.getMember().getMemberNumber(),
                        account.getAccountType().name(),
                        account.getBalance().doubleValue()
                );
            }
        }

        log.info("Completed low balance check for {} accounts", activeAccounts.size());
    }

    /**
     * Calculate interest for savings accounts - runs monthly on the 1st at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void calculateMonthlyInterest() {
        List<Account> savingsAccounts = accountRepository.findByAccountTypeAndStatus(
                Account.AccountType.SAVINGS, Account.AccountStatus.ACTIVE);

        for (Account account : savingsAccounts) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                // Calculate monthly interest (annual rate / 12)
                BigDecimal monthlyRate = account.getInterestRate().divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);
                BigDecimal interest = account.getBalance().multiply(monthlyRate);

                // Add interest to account
                account.setBalance(account.getBalance().add(interest));
                account.setUpdatedDate(LocalDateTime.now());
                accountRepository.save(account);

                // Send notification
                String message = String.format("Monthly interest of KSH %.2f has been added to your %s account. New balance: KSH %.2f",
                        interest.doubleValue(),
                        account.getAccountType().name(),
                        account.getBalance().doubleValue());

                notificationHelper.notifySystemNotification(
                        account.getMember().getMemberNumber(),
                        "Interest Added",
                        message,
                        Notification.NotificationType.SUCCESS,
                        1
                );
            }
        }

        log.info("Calculated monthly interest for {} savings accounts", savingsAccounts.size());
    }

    private void sendStatusChangeNotification(String memberNumber, Account account,
                                              Account.AccountStatus oldStatus, Account.AccountStatus newStatus, String reason) {
        String message;
        Notification.NotificationType notificationType;

        switch (newStatus) {
            case ACTIVE:
                if (oldStatus == Account.AccountStatus.FROZEN) {
                    message = String.format("Your %s account (%s) has been reactivated.",
                            account.getAccountType().name(), account.getAccountNumber());
                    notificationType = Notification.NotificationType.SUCCESS;
                } else {
                    message = String.format("Your %s account (%s) status changed to active.",
                            account.getAccountType().name(), account.getAccountNumber());
                    notificationType = Notification.NotificationType.INFO;
                }
                break;

            case FROZEN:
                message = String.format("Your %s account (%s) has been frozen. Reason: %s",
                        account.getAccountType().name(), account.getAccountNumber(), reason);
                notificationType = Notification.NotificationType.WARNING;
                break;

            case BLOCKED:
                message = String.format("Your %s account (%s) has been blocked. Reason: %s. Please contact support.",
                        account.getAccountType().name(), account.getAccountNumber(), reason);
                notificationType = Notification.NotificationType.ERROR;
                break;

            case CLOSED:
                message = String.format("Your %s account (%s) has been closed. Reason: %s",
                        account.getAccountType().name(), account.getAccountNumber(), reason);
                notificationType = Notification.NotificationType.INFO;
                break;

            default:
                message = String.format("Your %s account (%s) status has been updated.",
                        account.getAccountType().name(), account.getAccountNumber());
                notificationType = Notification.NotificationType.INFO;
        }

        int priority = (newStatus == Account.AccountStatus.BLOCKED || newStatus == Account.AccountStatus.FROZEN) ? 3 : 1;

        notificationHelper.notifySystemNotification(memberNumber, "Account Status Update", message, notificationType, priority);
    }

    private BigDecimal getMinimumBalanceForAccountType(Account.AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> new BigDecimal("1000");
            case CURRENT -> BigDecimal.ZERO;
            case SHARE_CAPITAL -> new BigDecimal("5000");
            case FIXED_DEPOSIT -> new BigDecimal("10000");
        };
    }

    private BigDecimal getInterestRateForAccountType(Account.AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> new BigDecimal("0.05"); // 5% annual
            case CURRENT -> BigDecimal.ZERO;
            case SHARE_CAPITAL -> new BigDecimal("0.08"); // 8% annual
            case FIXED_DEPOSIT -> new BigDecimal("0.12"); // 12% annual
        };
    }

    private String generateAccountNumber(Account.AccountType accountType) {
        String prefix = switch (accountType) {
            case SAVINGS -> "SAV";
            case CURRENT -> "CUR";
            case SHARE_CAPITAL -> "SHR";
            case FIXED_DEPOSIT -> "FXD";
        };

        // Generate unique account number with timestamp and random component
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String random = String.valueOf((int)(Math.random() * 1000));

        return prefix + timestamp + String.format("%03d", Integer.parseInt(random));
    }
    // Add these methods to your existing AccountService class

    /**
     * Get specific account by ID for a member
     */
//    @Transactional(readOnly = true)
//    public AccountResponse getAccount(String memberNumber, Long accountId) {
//        log.info("Getting account {} for member: {}", accountId, memberNumber);
//
//        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
//                .orElseThrow(() -> {
//                    log.error("Account {} not found for member: {}", accountId, memberNumber);
//                    return new BadRequestException("Account not found or doesn't belong to member");
//                });
//
//        log.info("Found account: {} - {} for member: {}",
//                account.getAccountNumber(), account.getAccountType(), memberNumber);
//
//        return AccountResponse.fromEntity(account);
//    }

    /**
     * Get account by account number for a member
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String memberNumber, String accountNumber) {
        log.info("Getting account {} for member: {}", accountNumber, memberNumber);

        Account account = accountRepository.findByAccountNumberAndMemberMemberNumber(accountNumber, memberNumber)
                .orElseThrow(() -> {
                    log.error("Account {} not found for member: {}", accountNumber, memberNumber);
                    return new BadRequestException("Account not found or doesn't belong to member");
                });

        log.info("Found account: {} - {} for member: {}",
                account.getAccountNumber(), account.getAccountType(), memberNumber);

        return AccountResponse.fromEntity(account);
    }

    /**
     * Get accounts available for deposit (excludes savings account from source options)
     */
    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> getDepositSourceAccounts(Member member) {
        log.info("Getting deposit source accounts for member: {}", member.getMemberNumber());

        List<Account> accounts = accountRepository.findByMember(member);
        log.info("Found {} total accounts for member: {}", accounts.size(), member.getMemberNumber());

        List<AccountSummaryResponse> result = accounts.stream()
                .filter(account -> {
                    // Only include active accounts
                    boolean isActive = account.getStatus() == Account.AccountStatus.ACTIVE;
                    // Exclude savings accounts as source (you can't transfer from savings to savings)
                    boolean isNotSavings = account.getAccountType() != Account.AccountType.SAVINGS;
                    // Check if account has balance above minimum
                    boolean hasUsableBalance = account.getBalance().compareTo(
                            account.getMinimumBalance().add(new BigDecimal("10"))) > 0;

                    log.debug("Account {}: type={}, status={}, isActive={}, isNotSavings={}, hasUsableBalance={}",
                            account.getAccountNumber(), account.getAccountType(), account.getStatus(),
                            isActive, isNotSavings, hasUsableBalance);

                    return isActive && isNotSavings && hasUsableBalance;
                })
                .map(account -> AccountSummaryResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getAccountType().name())
                        .balance(account.getBalance())
                        .minimumBalance(account.getMinimumBalance())
                        .interestRate(account.getInterestRate())
                        .status(account.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        log.info("Returning {} eligible deposit source accounts for member: {}",
                result.size(), member.getMemberNumber());

        return result;
    }

    /**
     * Get all accounts for a member (enhanced version of existing method)
     */
//    @Transactional(readOnly = true)
//    public List<AccountResponse> getAccounts(String memberNumber) {
//        log.info("Getting all accounts for member: {}", memberNumber);
//
//        List<Account> accounts = accountRepository.findByMemberMemberNumberAndStatusOrderByCreatedDateDesc(
//                memberNumber, Account.AccountStatus.ACTIVE);
//
//        log.info("Found {} active accounts for member: {}", accounts.size(), memberNumber);
//
//        // Log account details for debugging
//        accounts.forEach(account ->
//                log.debug("Account found: {} - {} - Balance: {} - Status: {}",
//                        account.getAccountNumber(), account.getAccountType(),
//                        account.getBalance(), account.getStatus()));
//
//        return accounts.stream()
//                .map(AccountResponse::fromEntity)
//                .collect(Collectors.toList());
//    }

    /**
     * Get all accounts for a member (including inactive ones) - for admin purposes
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccountsForMember(String memberNumber) {
        log.info("Getting all accounts (including inactive) for member: {}", memberNumber);

        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        List<Account> accounts = accountRepository.findByMember(member);

        log.info("Found {} total accounts for member: {}", accounts.size(), memberNumber);

        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
}