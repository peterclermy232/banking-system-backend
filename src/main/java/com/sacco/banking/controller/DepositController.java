package com.sacco.banking.controller;

import com.sacco.banking.dto.request.DepositRequest;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deposits")
@RequiredArgsConstructor
@Tag(name = "Deposits", description = "Deposit management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DepositController {

    private final DepositService depositService;
    private final MemberRepository memberRepository;

    @PostMapping("/cash")
    @Operation(summary = "Cash deposit", description = "Process cash deposit to member account")
    public ResponseEntity<TransactionResponse> cashDeposit(
            @Valid @RequestBody DepositRequest depositRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = depositService.processCashDeposit(depositRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account-to-account")
    @Operation(summary = "Account to account deposit", description = "Transfer money between member's own accounts")
    public ResponseEntity<TransactionResponse> accountToAccountDeposit(
            @Valid @RequestBody DepositRequest depositRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = depositService.processAccountToAccountDeposit(depositRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mpesa")
    @Operation(summary = "M-Pesa deposit", description = "Process M-Pesa deposit to member account")
    public ResponseEntity<TransactionResponse> mpesaDeposit(
            @Valid @RequestBody DepositRequest depositRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = depositService.processMpesaDeposit(depositRequest, member);
        return ResponseEntity.ok(response);
    }
}