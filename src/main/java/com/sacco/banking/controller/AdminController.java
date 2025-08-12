package com.sacco.banking.controller;

import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Admin management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create admin user", description = "Create a new admin user (Admin only)")
    public ResponseEntity<MemberResponse> createAdmin(@RequestBody @Valid RegisterRequest request) {
        MemberResponse admin = adminService.createAdmin(request);
        return ResponseEntity.ok(admin);
    }

    @PostMapping("/promote-to-admin/{memberNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promote member to admin", description = "Promote existing member to admin role")
    public ResponseEntity<MemberResponse> promoteToAdmin(@PathVariable String memberNumber) {
        MemberResponse admin = adminService.promoteToAdmin(memberNumber);
        return ResponseEntity.ok(admin);
    }

    @PostMapping("/demote-to-member/{memberNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Demote admin to member", description = "Demote admin to regular member role")
    public ResponseEntity<MemberResponse> demoteToMember(@PathVariable String memberNumber) {
        MemberResponse member = adminService.demoteToMember(memberNumber);
        return ResponseEntity.ok(member);
    }
}