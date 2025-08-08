package com.sacco.banking.service;

import com.sacco.banking.dto.request.CreateAccountRequest;
import com.sacco.banking.dto.response.AccountResponse;
import com.sacco.banking.dto.response.AccountSummaryResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Notification;
import com.sacco.banking.enums.AccountStatus;
import com.sacco.banking.enums.AccountType;
import com.sacco.banking.enums.NotificationType;
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
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        if (accountRepository.existsByMemberMemberNumberAndAccountType(memberNumber, request.getAccountType())) {
            throw new BadRequestException("Member already has a " + request.getAccountType() + " account");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber(request.getAccountType()))
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .minimumBalance(getMinimumBalanceForAccountType(request.getAccountType()))
                .interestRate(getInterestRateForAccountType(request.getAccountType()))
                .member(member)
                .status(AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        account = accountRepository.save(account);
        notificationHelper.notifyAccountCreated(memberNumber, request.getAccountType().name());

        log.info("Created {} account {} for member {}", request.getAccountType(), account.getAccountNumber(), memberNumber);

        return AccountResponse.fromEntity(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(String memberNumber) {
        return accountRepository.findByMemberMemberNumberAndStatusOrderByCreatedDateDesc(memberNumber, AccountStatus.ACTIVE)
                .stream().map(AccountResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String memberNumber, Long accountId) {
        return accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .map(AccountResponse::fromEntity)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String memberNumber, String accountNumber) {
        return accountRepository.findByAccountNumberAndMemberMemberNumber(accountNumber, memberNumber)
                .map(AccountResponse::fromEntity)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));
    }

    @Transactional
    public AccountResponse updateAccountStatus(String memberNumber, Long accountId, AccountStatus status, String reason) {
        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(status);
        account.setUpdatedDate(LocalDateTime.now());
        account = accountRepository.save(account);

        sendStatusChangeNotification(memberNumber, account, oldStatus, status, reason);

        log.info("Updated account {} status from {} to {} for member {}. Reason: {}",
                account.getAccountNumber(), oldStatus, status, memberNumber, reason);

        return AccountResponse.fromEntity(account);
    }

    @Transactional
    public void closeAccount(String memberNumber, Long accountId, String reason) {
        Account account = accountRepository.findByIdAndMemberMemberNumber(accountId, memberNumber)
                .orElseThrow(() -> new BadRequestException("Account not found or doesn't belong to member"));

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot close account with positive balance. Please withdraw all funds first.");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setClosedDate(LocalDateTime.now());
        account.setUpdatedDate(LocalDateTime.now());
        accountRepository.save(account);

        notificationHelper.notifyAccountNotification(memberNumber, account.getAccountType().name(),
                String.format("Your %s account (%s) has been closed. Reason: %s",
                        account.getAccountType().name(), account.getAccountNumber(), reason),
                NotificationType.INFO);

        log.info("Closed account {} for member {}. Reason: {}", account.getAccountNumber(), memberNumber, reason);
    }

    @Transactional
    public void freezeAccount(String memberNumber, Long accountId, String reason) {
        updateAccountStatus(memberNumber, accountId, AccountStatus.FROZEN, reason);
    }

    @Transactional
    public void unfreezeAccount(String memberNumber, Long accountId) {
        updateAccountStatus(memberNumber, accountId, AccountStatus.ACTIVE, "Account unfrozen by member request");
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(String memberNumber) {
        return accountRepository.findByMemberMemberNumberAndStatus(memberNumber, AccountStatus.ACTIVE)
                .stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(int page, int size) {
        return accountRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(page, size))
                .map(AccountResponse::fromEntity);
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkLowBalances() {
        List<Account> accounts = accountRepository.findByStatus(AccountStatus.ACTIVE);

        accounts.stream().filter(account -> {
            BigDecimal threshold = account.getMinimumBalance().multiply(BigDecimal.valueOf(1.1));
            return account.getBalance().compareTo(threshold) <= 0 &&
                    account.getBalance().compareTo(account.getMinimumBalance()) > 0;
        }).forEach(account -> notificationHelper.notifyLowBalance(
                account.getMember().getMemberNumber(),
                account.getAccountType().name(),
                account.getBalance().doubleValue()));

        log.info("Completed low balance check for {} accounts", accounts.size());
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void calculateMonthlyInterest() {
        List<Account> accounts = accountRepository.findByAccountTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE);

        accounts.stream().filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .forEach(account -> {
                    BigDecimal monthlyRate = account.getInterestRate().divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
                    BigDecimal interest = account.getBalance().multiply(monthlyRate);
                    account.setBalance(account.getBalance().add(interest));
                    account.setUpdatedDate(LocalDateTime.now());
                    accountRepository.save(account);

                    notificationHelper.notifySystemNotification(
                            account.getMember().getMemberNumber(),
                            "Interest Added",
                            String.format("Monthly interest of KSH %.2f has been added to your %s account. New balance: KSH %.2f",
                                    interest.doubleValue(), account.getAccountType().name(), account.getBalance().doubleValue()),
                            NotificationType.SUCCESS,
                            1);
                });

        log.info("Calculated monthly interest for {} savings accounts", accounts.size());
    }

    private void sendStatusChangeNotification(String memberNumber, Account account, AccountStatus oldStatus, AccountStatus newStatus, String reason) {
        String message;
        NotificationType type;

        switch (newStatus) {
            case ACTIVE -> {
                message = oldStatus == AccountStatus.FROZEN
                        ? String.format("Your %s account (%s) has been reactivated.", account.getAccountType(), account.getAccountNumber())
                        : String.format("Your %s account (%s) is now active.", account.getAccountType(), account.getAccountNumber());
                type = NotificationType.SUCCESS;
            }
            case FROZEN -> {
                message = String.format("Your %s account (%s) has been frozen. Reason: %s", account.getAccountType(), account.getAccountNumber(), reason);
                type = NotificationType.WARNING;
            }
            case BLOCKED -> {
                message = String.format("Your %s account (%s) has been blocked. Reason: %s", account.getAccountType(), account.getAccountNumber(), reason);
                type = NotificationType.ERROR;
            }
            case CLOSED -> {
                message = String.format("Your %s account (%s) has been closed. Reason: %s", account.getAccountType(), account.getAccountNumber(), reason);
                type = NotificationType.INFO;
            }
            default -> {
                message = String.format("Your %s account (%s) status updated.", account.getAccountType(), account.getAccountNumber());
                type = NotificationType.INFO;
            }
        }

        notificationHelper.notifySystemNotification(memberNumber, "Account Status Update", message, type, 1);
    }

    private BigDecimal getMinimumBalanceForAccountType(AccountType type) {
        return switch (type) {
            case SAVINGS -> BigDecimal.valueOf(1000);
            case CURRENT -> BigDecimal.ZERO;
            case SHARE_CAPITAL -> BigDecimal.valueOf(5000);
            case FIXED_DEPOSIT -> BigDecimal.valueOf(10000);
            case LOAN -> BigDecimal.ZERO;
        };
    }

    private BigDecimal getInterestRateForAccountType(AccountType type) {
        return switch (type) {
            case SAVINGS -> BigDecimal.valueOf(0.05);
            case CURRENT -> BigDecimal.ZERO;
            case SHARE_CAPITAL -> BigDecimal.valueOf(0.08);
            case FIXED_DEPOSIT -> BigDecimal.valueOf(0.12);
            case LOAN -> BigDecimal.ZERO;
        };
    }

    private String generateAccountNumber(AccountType type) {
        String prefix = switch (type) {
            case SAVINGS -> "SAV";
            case CURRENT -> "CUR";
            case SHARE_CAPITAL -> "SHR";
            case FIXED_DEPOSIT -> "FXD";
            case LOAN -> "LOA";
        };

        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String random = String.format("%03d", (int) (Math.random() * 1000));
        return prefix + timestamp + random;
    }

    @Transactional(readOnly = true)
    public List<AccountSummaryResponse> getDepositSourceAccounts(Member member) {
        return accountRepository.findByMember(member).stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE
                        && account.getAccountType() != AccountType.SAVINGS
                        && account.getBalance().compareTo(account.getMinimumBalance().add(BigDecimal.TEN)) > 0)
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
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccountsForMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        return accountRepository.findByMember(member).stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
}