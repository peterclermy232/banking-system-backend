package com.sacco.banking.service;

import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by member number first, then by email
        Member member = memberRepository.findByMemberNumber(username)
                .or(() -> memberRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Check if member is active
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            throw new UsernameNotFoundException("Account is " + member.getStatus().toString().toLowerCase());
        }
        // For now, all members have USER role. You can extend this based on your requirements
        return new UserPrincipal(member);
    }

    public UserDetails loadUserByMemberNumber(String memberNumber) throws UsernameNotFoundException {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found with member number: " + memberNumber));

        return User.builder()
                .username(member.getMemberNumber())
                .password("") // Password handling will be done separately
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(member.getStatus() != Member.MemberStatus.ACTIVE)
                .credentialsExpired(false)
                .disabled(member.getStatus() == Member.MemberStatus.TERMINATED)
                .build();
    }
}