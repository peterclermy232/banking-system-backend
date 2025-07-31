package com.sacco.banking.controller;

import com.sacco.banking.dto.response.AccountResponse;
import com.sacco.banking.dto.response.AccountSummaryResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /**
     * Get all accounts for the authenticated member
     * This is what your Angular service calls: getMemberAccounts()
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMemberAccounts(@AuthenticationPrincipal Member member) {
        try {
            log.info("Getting accounts for member: {}", member.getMemberNumber());
            List<AccountResponse> accounts = accountService.getAccounts(member.getMemberNumber());
            log.info("Found {} accounts for member: {}", accounts.size(), member.getMemberNumber());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error getting accounts for member {}: {}", member.getMemberNumber(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get accounts available for deposit (excludes savings account from source options)
     * This is more specific for savings deposits
     */
    @GetMapping("/deposit-sources")
    public ResponseEntity<List<AccountSummaryResponse>> getDepositSourceAccounts(@AuthenticationPrincipal Member member) {
        try {
            log.info("Getting deposit source accounts for member: {}", member.getMemberNumber());
            List<AccountSummaryResponse> accounts = accountService.getDepositSourceAccounts(member);
            log.info("Found {} deposit source accounts for member: {}", accounts.size(), member.getMemberNumber());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error getting deposit source accounts for member {}: {}", member.getMemberNumber(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get specific account by ID
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal Member member) {

        AccountResponse account = accountService.getAccount(member.getMemberNumber(), accountId);
        return ResponseEntity.ok(account);
    }

    /**
     * Get account by account number
     */
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Member member) {

        AccountResponse account = accountService.getAccountByNumber(member.getMemberNumber(), accountNumber);
        return ResponseEntity.ok(account);
    }
}