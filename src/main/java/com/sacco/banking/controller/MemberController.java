package com.sacco.banking.controller;

import com.sacco.banking.dto.request.UpdateMemberRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.dto.response.MemberStatsResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get member profile", description = "Retrieve authenticated member's profile")
    public ResponseEntity<MemberResponse> getMemberProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MemberResponse response = MemberResponse.fromEntity(member);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all members", description = "Retrieve paginated list of all members")
    public ResponseEntity<Page<MemberResponse>> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        Page<MemberResponse> members = memberService.getAllMembers(PageRequest.of(page, size), search);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get member statistics", description = "Retrieve SACCO member statistics")
    public ResponseEntity<MemberStatsResponse> getMemberStats() {
        MemberStatsResponse stats = memberService.getMemberStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{memberNumber}")
    @Operation(summary = "Get member by number", description = "Retrieve member details by member number")
    public ResponseEntity<MemberResponse> getMemberByNumber(@PathVariable String memberNumber) {
        MemberResponse member = memberService.getMemberByNumber(memberNumber);
        return ResponseEntity.ok(member);
    }

    @PatchMapping("/{memberNumber}/suspend")
    @Operation(summary = "Suspend member", description = "Suspend a member account")
    public ResponseEntity<MemberResponse> suspendMember(
            @Parameter(name = "memberNumber", description = "Member number")
            @PathVariable String memberNumber) {
        MemberResponse suspendedMember = memberService.suspendMember(memberNumber);
        return ResponseEntity.ok(suspendedMember);
    }

    @PatchMapping("/{memberNumber}/activate")
    @Operation(summary = "Activate member", description = "Activate a suspended member account")
    public ResponseEntity<MemberResponse> activateMember(
            @Parameter(name = "memberNumber", description = "Member number")
            @PathVariable String memberNumber) {
        MemberResponse activatedMember = memberService.activateMember(memberNumber);
        return ResponseEntity.ok(activatedMember);
    }

    @DeleteMapping("/{memberNumber}")
    @Operation(summary = "Delete member", description = "Delete a member account (soft delete)")
    public ResponseEntity<Void> deleteMember(
            @Parameter(name = "memberNumber", description = "Member number")
            @PathVariable String memberNumber) {
        memberService.deleteMember(memberNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/total-savings")
    @Operation(summary = "Get total savings", description = "Retrieve the sum of all members' total savings")
    public ResponseEntity<BigDecimal> getTotalSavings() {
        BigDecimal totalSavings = memberService.getTotalSavings();
        return ResponseEntity.ok(totalSavings);
    }

    @PutMapping("/{memberNumber}")
    @Operation(summary = "Update member", description = "Update member information")
    public ResponseEntity<MemberResponse> updateMember(
            @Parameter(name = "memberNumber", description = "Member number")
            @PathVariable String memberNumber,
            @RequestBody @Valid UpdateMemberRequest request) {

        MemberResponse updatedMember = memberService.updateMember(memberNumber, request);
        return ResponseEntity.ok(updatedMember);
    }

    @PatchMapping("/{memberNumber}")
    @Operation(summary = "Partially update member", description = "Partially update member information")
    public ResponseEntity<MemberResponse> partialUpdateMember(
            @Parameter(name = "memberNumber", description = "Member number")
            @PathVariable String memberNumber,
            @RequestBody UpdateMemberRequest request) {

        MemberResponse updatedMember = memberService.partialUpdateMember(memberNumber, request);
        return ResponseEntity.ok(updatedMember);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update own profile", description = "Update authenticated member's profile")
    public ResponseEntity<MemberResponse> updateOwnProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid UpdateMemberRequest request) {

        MemberResponse updatedMember = memberService.updateMember(userPrincipal.getMemberNumber(), request);
        return ResponseEntity.ok(updatedMember);
    }

    @PatchMapping("/profile")
    @Operation(summary = "Partially update own profile", description = "Partially update authenticated member's profile")
    public ResponseEntity<MemberResponse> partialUpdateOwnProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UpdateMemberRequest request) {

        MemberResponse updatedMember = memberService.partialUpdateMember(userPrincipal.getMemberNumber(), request);
        return ResponseEntity.ok(updatedMember);
    }

}