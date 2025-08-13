package com.sacco.banking.service;

import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Role;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.repository.RoleRepository;
import com.sacco.banking.util.MemberNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberNumberGenerator memberNumberGenerator;
    private final NotificationHelper notificationHelper;

    @Value("${app.admin.max-admins:10}")
    private int maxAdmins;

    public MemberResponse createAdmin(RegisterRequest request) {
        // Security check: Limit number of admins
        long currentAdminCount = memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN, Member.MemberStatus.ACTIVE);
        if (currentAdminCount >= maxAdmins) {
            throw new BadRequestException("Maximum number of administrators reached. Contact system administrator.");
        }

        // Validate admin email domain
        if (!isValidAdminEmail(request.getEmail())) {
            throw new BadRequestException("Admin registration requires authorized email domain");
        }

        // Check if member number already exists (shouldn't happen with auto-generation)
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        // Generate admin member number
        String memberNumber = memberNumberGenerator.generateMemberNumber(true);

        // Create new admin member
        Member admin = new Member();
        admin.setMemberNumber(memberNumber);
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setNationalId(request.getNationalId());
        admin.setAddress(request.getAddress());
        admin.setOccupation(request.getOccupation());
        admin.setDateOfBirth(request.getDateOfBirth());
        admin.setIdNumber(request.getIdNumber());
        admin.setShareCapital(BigDecimal.ZERO);
        admin.setMonthlyIncome(request.getMonthlyIncome() != null ? request.getMonthlyIncome() : 0);
        admin.setDateJoined(LocalDateTime.now());
        admin.setStatus(Member.MemberStatus.ACTIVE);
        admin.setCreditScore(800); // Admins get higher credit score

        // Assign both ADMIN and MEMBER roles
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
        Role memberRole = roleRepository.findByName(RoleName.ROLE_MEMBER)
                .orElseThrow(() -> new RuntimeException("Member role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(memberRole);
        admin.setRoles(roles);

        Member savedAdmin = memberRepository.save(admin);

        // Send welcome notification
        notificationHelper.notifyWelcome(savedAdmin.getMemberNumber(), savedAdmin.getFirstName());

        log.info("Created admin account {} for {}", savedAdmin.getMemberNumber(), savedAdmin.getEmail());

        return MemberResponse.fromEntity(savedAdmin);
    }

    public MemberResponse promoteToAdmin(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberNumber));

        // Security check: Limit number of admins
        long currentAdminCount = memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN, Member.MemberStatus.ACTIVE);
        if (currentAdminCount >= maxAdmins) {
            throw new BadRequestException("Maximum number of administrators reached");
        }

        // Check if member is already admin
        if (member.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN)) {
            throw new BadRequestException("Member is already an administrator");
        }

        // Get admin role
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));

        // Add admin role to existing roles
        member.getRoles().add(adminRole);
        member.setUpdatedDate(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        // Notify member about promotion
        notificationHelper.notifySystemNotification(
                memberNumber,
                "Account Promoted",
                "Congratulations! Your account has been promoted to Administrator level. You now have access to admin features.",
                com.sacco.banking.enums.NotificationType.SUCCESS,
                2
        );

        log.info("Promoted member {} to administrator", memberNumber);

        return MemberResponse.fromEntity(savedMember);
    }

    public MemberResponse demoteToMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberNumber));

        // Prevent demoting the last admin
        long currentAdminCount = memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN, Member.MemberStatus.ACTIVE);
        if (currentAdminCount <= 1) {
            throw new BadRequestException("Cannot demote the last administrator. System must have at least one admin.");
        }

        // Get roles
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
        Role memberRole = roleRepository.findByName(RoleName.ROLE_MEMBER)
                .orElseThrow(() -> new RuntimeException("Member role not found"));

        // Remove admin role and ensure member role
        member.getRoles().remove(adminRole);
        member.getRoles().add(memberRole);
        member.setUpdatedDate(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        // Notify member about demotion
        notificationHelper.notifySystemNotification(
                memberNumber,
                "Admin Access Removed",
                "Your administrator privileges have been revoked. You now have standard member access.",
                com.sacco.banking.enums.NotificationType.WARNING,
                2
        );

        log.info("Demoted administrator {} to regular member", memberNumber);

        return MemberResponse.fromEntity(savedMember);
    }

    public long getAdminCount() {
        return memberRepository.countByRoleAndStatus(RoleName.ROLE_ADMIN, Member.MemberStatus.ACTIVE);
    }

    public boolean canCreateMoreAdmins() {
        return getAdminCount() < maxAdmins;
    }

    private boolean isValidAdminEmail(String email) {
        String[] allowedDomains = {"admin.sacco.com", "board.sacco.com", "executive.sacco.com"};
        String domain = email.substring(email.lastIndexOf("@") + 1);

        for (String allowedDomain : allowedDomains) {
            if (domain.equalsIgnoreCase(allowedDomain)) {
                return true;
            }
        }
        return false;
    }
}