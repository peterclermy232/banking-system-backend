package com.sacco.banking.controller;

import com.sacco.banking.dto.request.LoanApplicationRequest;
import com.sacco.banking.dto.request.LoanCalculationRequest;
import com.sacco.banking.dto.response.LoanResponse;
import com.sacco.banking.dto.response.LoanCalculationResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class LoanController {

    private final LoanService loanService;
    private final MemberRepository memberRepository;

    @GetMapping
    @Operation(summary = "Get member loans", description = "Retrieve all loans for the authenticated member")
    public ResponseEntity<List<LoanResponse>> getMemberLoans(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<LoanResponse> loans = loanService.getMemberLoans(member);
        return ResponseEntity.ok(loans);
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for loan", description = "Submit a new loan application")
    public ResponseEntity<LoanResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest loanRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        LoanResponse response = loanService.applyForLoan(loanRequest, member);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate loan", description = "Calculate loan payment details")
    public ResponseEntity<LoanCalculationResponse> calculateLoan(@Valid @RequestBody LoanCalculationRequest request) {
        LoanCalculationResponse calculation = loanService.calculateLoan(request);
        return ResponseEntity.ok(calculation);
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan details", description = "Retrieve details of a specific loan")
    public ResponseEntity<LoanResponse> getLoanDetails(
            @PathVariable Long loanId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        LoanResponse loan = loanService.getLoanDetails(loanId, member);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/types")
    @Operation(summary = "Get loan types", description = "Retrieve all available loan types")
    public ResponseEntity<List<String>> getLoanTypes() {
        List<String> loanTypes = List.of(com.sacco.banking.enums.LoanType.values())
                .stream()
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(loanTypes);
    }

}