package com.sacco.banking.service;

import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Role;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberResponse createAdmin(RegisterRequest request) {
        // Check if member number already exists
        if (memberRepository.existsByMemberNumber(request.getMemberNumber())) {
            throw new RuntimeException("Member number already exists: " + request.getMemberNumber());
        }

        // Check if email already exists
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Create new admin member
        Member admin = new Member();
        admin.setMemberNumber(request.getMemberNumber());
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setNationalId(request.getNationalId());
        admin.setAddress(request.getAddress());
        admin.setOccupation(request.getOccupation());
        admin.setUpdatedDate(LocalDateTime.now());
        admin.setUpdatedDate(LocalDateTime.now());


        // Assign ADMIN role
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        Member savedAdmin = memberRepository.save(admin);
        return MemberResponse.fromEntity(savedAdmin);
    }

    public MemberResponse promoteToAdmin(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberNumber));

        // Get admin role
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));

        // Add admin role to existing roles
        member.getRoles().add(adminRole);
        member.setUpdatedDate(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);
        return MemberResponse.fromEntity(savedMember);
    }

    public MemberResponse demoteToMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberNumber));

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
        return MemberResponse.fromEntity(savedMember);
    }
}