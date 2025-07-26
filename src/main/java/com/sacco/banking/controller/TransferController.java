package com.sacco.banking.controller;

import com.sacco.banking.dto.request.TransferRequest;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Money Transfer", description = "Money transfer management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransferController {

    private final TransferService transferService;
    private final MemberRepository memberRepository;

    @PostMapping("/internal")
    @Operation(summary = "Internal transfer", description = "Transfer money between SACCO member accounts")
    public ResponseEntity<TransactionResponse> internalTransfer(
            @Valid @RequestBody TransferRequest transferRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = transferService.processInternalTransfer(transferRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/external")
    @Operation(summary = "External transfer", description = "Transfer money to external bank accounts")
    public ResponseEntity<TransactionResponse> externalTransfer(
            @Valid @RequestBody TransferRequest transferRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = transferService.processExternalTransfer(transferRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mpesa")
    @Operation(summary = "M-Pesa transfer", description = "Transfer money via M-Pesa")
    public ResponseEntity<TransactionResponse> mpesaTransfer(
            @Valid @RequestBody TransferRequest transferRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = transferService.processMpesaTransfer(transferRequest, member);
        return ResponseEntity.ok(response);
    }
}