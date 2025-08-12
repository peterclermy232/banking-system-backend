package com.sacco.banking.controller;

import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class InitialAdminController {

    private final AdminService adminService;
    private final MemberRepository memberRepository;

    @PostMapping("/create-first-admin")
    @Operation(summary = "Create first admin", description = "Create the very first admin (only works if no members exist)")
    public ResponseEntity<?> createFirstAdmin(@RequestBody @Valid RegisterRequest request) {
        // Only allow if no members exist in the system
        long memberCount = memberRepository.count();
        if (memberCount > 0) {
            return ResponseEntity.badRequest()
                    .body("Cannot create admin. Members already exist in the system. Use the regular admin creation endpoint.");
        }

        try {
            MemberResponse admin = adminService.createAdmin(request);
            return ResponseEntity.ok(admin);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating admin: " + e.getMessage());
        }
    }

    @GetMapping("/system-status")
    @Operation(summary = "Check system status", description = "Check if any admins exist in the system")
    public ResponseEntity<?> getSystemStatus() {
        long totalMembers = memberRepository.count();
        // You might want to add a method to count admins specifically

        return ResponseEntity.ok(new SystemStatus(totalMembers, totalMembers == 0));
    }

    // Inner class for system status response
    public static class SystemStatus {
        public final long totalMembers;
        public final boolean canCreateFirstAdmin;

        public SystemStatus(long totalMembers, boolean canCreateFirstAdmin) {
            this.totalMembers = totalMembers;
            this.canCreateFirstAdmin = canCreateFirstAdmin;
        }
    }
}