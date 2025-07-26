package com.sacco.banking.controller;

import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.dto.response.MemberStatsResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
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
}