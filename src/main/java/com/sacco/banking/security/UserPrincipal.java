package com.sacco.banking.security;

import com.sacco.banking.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final Member member;

    public UserPrincipal(Member member) {
        super(
                member.getMemberNumber(),
                member.getPassword(),
                mapRolesToAuthorities(member)
        );
        this.member = member;
    }

    private static Collection<? extends GrantedAuthority> mapRolesToAuthorities(Member member) {
        try {
            if (member.getRoles() != null && !member.getRoles().isEmpty()) {
                return member.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().toString()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // If lazy loading fails, return default role
            System.err.println("Failed to load roles for member: " + member.getMemberNumber() + ". Using default role.");
        }

        // Return default role if roles cannot be loaded
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }

    public String getMemberNumber() {
        return member.getMemberNumber();
    }

    public Member getMember() {
        return member;
    }

    // Helper methods with null checks
    public boolean hasRole(String roleName) {
        try {
            return getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(roleName));
        } catch (Exception e) {
            return false;
        }
    }

    // Helper method to check if user is admin
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    // Helper method to check if user is member
    public boolean isMember() {
        return hasRole("ROLE_MEMBER");
    }
}