package com.sacco.banking.controller;

import com.sacco.banking.dto.response.DashboardResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import com.sacco.banking.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final DashboardService dashboardService;
    private final MemberRepository memberRepository;

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Retrieve member dashboard information")
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Member member = memberRepository.findByMemberNumber(userPrincipal.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        DashboardResponse dashboardData = dashboardService.getDashboardData(member);
        return ResponseEntity.ok(dashboardData);
    }
}