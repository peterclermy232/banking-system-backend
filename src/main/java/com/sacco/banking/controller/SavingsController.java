package com.sacco.banking.controller;

import com.sacco.banking.dto.request.SavingsGoalRequest;
import com.sacco.banking.dto.request.SavingsDepositRequest;
import com.sacco.banking.dto.response.SavingsGoalResponse;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.SavingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/savings")
@RequiredArgsConstructor
@Tag(name = "Savings", description = "Savings and goals management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SavingsController {

    private final SavingsService savingsService;
    private final MemberRepository memberRepository;

    @GetMapping("/goals")
    @Operation(summary = "Get savings goals", description = "Retrieve all savings goals for the member")
    public ResponseEntity<List<SavingsGoalResponse>> getSavingsGoals(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<SavingsGoalResponse> goals = savingsService.getMemberSavingsGoals(member);
        return ResponseEntity.ok(goals);
    }

    @PostMapping("/goals")
    @Operation(summary = "Create savings goal", description = "Create a new savings goal")
    public ResponseEntity<SavingsGoalResponse> createSavingsGoal(
            @Valid @RequestBody SavingsGoalRequest goalRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SavingsGoalResponse response = savingsService.createSavingsGoal(goalRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Make savings deposit", description = "Deposit money to savings account")
    public ResponseEntity<TransactionResponse> makeSavingsDeposit(
            @Valid @RequestBody SavingsDepositRequest depositRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        TransactionResponse response = savingsService.makeSavingsDeposit(depositRequest, member);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/goals/{goalId}")
    @Operation(summary = "Update savings goal", description = "Update an existing savings goal")
    public ResponseEntity<SavingsGoalResponse> updateSavingsGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody SavingsGoalRequest goalRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SavingsGoalResponse response = savingsService.updateSavingsGoal(goalId, goalRequest, member);
        return ResponseEntity.ok(response);
    }
}