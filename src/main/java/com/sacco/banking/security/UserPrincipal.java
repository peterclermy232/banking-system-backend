package com.sacco.banking.security;

import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {
    private final String memberNumber;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final Member.MemberStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Member member) {
        this.memberNumber = member.getMemberNumber();
        this.email = member.getEmail();
        this.password = member.getPassword();
        this.firstName = member.getFirstName();
        this.lastName = member.getLastName();
        this.status = member.getStatus();
        this.authorities = mapRolesToAuthorities(member.getRoles());
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return memberNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != Member.MemberStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == Member.MemberStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

//    public boolean isMember() {
//        return hasRole("ROLE_MEMBER");
//    }
}