package com.sacco.banking.service;

import com.sacco.banking.entity.Member;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String memberNumber) throws UsernameNotFoundException {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found with number: " + memberNumber));

        return UserPrincipal.create(member);
    }
}