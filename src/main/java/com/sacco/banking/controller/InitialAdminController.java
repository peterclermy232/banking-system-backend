package com.sacco.banking.controller;

import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class InitialAdminController {

    private final AdminService adminService;
    private final MemberRepository memberRepository;

    @PostMapping("/create-first-admin")
    @Operation(summary = "Create first admin", description = "Create the very first admin (only works if no admins exist)")
    public ResponseEntity<?> createFirstAdmin(
            @RequestBody @Valid RegisterRequest request,
            @RequestParam String secretKey) {

        // Secret key check for initial admin creation
        if (!"SACCO_INITIAL_SETUP_2024".equals(secretKey)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid secret key for initial setup"));
        }

        // Only allow if no admins exist in the system
        long adminCount = memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN,
                com.sacco.banking.entity.Member.MemberStatus.ACTIVE);
        if (adminCount > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admins already exist in the system. Use the regular admin creation endpoint."));
        }

        // Validate email domain for first admin
        if (!request.getEmail().endsWith("@admin.sacco.com") &&
                !request.getEmail().endsWith("@board.sacco.com")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "First admin must use authorized email domain"));
        }

        try {
            MemberResponse admin = adminService.createAdmin(request);
            return ResponseEntity.ok(Map.of(
                    "message", "First admin created successfully",
                    "admin", admin
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error creating admin: " + e.getMessage()));
        }
    }

    @GetMapping("/system-status")
    @Operation(summary = "Check system status", description = "Check system initialization status")
    public ResponseEntity<?> getSystemStatus() {
        long totalMembers = memberRepository.count();
        long adminCount = memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN,
                com.sacco.banking.entity.Member.MemberStatus.ACTIVE);
        boolean canCreateFirstAdmin = adminCount == 0;

        return ResponseEntity.ok(Map.of(
                "totalMembers", totalMembers,
                "adminCount", adminCount,
                "canCreateFirstAdmin", canCreateFirstAdmin,
                "systemInitialized", adminCount > 0,
                "maxAdmins", 10
        ));
    }
}