package com.sacco.banking.util;

import com.sacco.banking.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberNumberGenerator {

    private final MemberRepository memberRepository;

    public synchronized String generateMemberNumber(boolean isAdmin) {
        String prefix = isAdmin ? "ADM" : "MB";

        // Get the current count of members with this prefix
        long count = memberRepository.countByMemberNumberStartingWith(prefix);

        // Generate the next number with proper padding
        String memberNumber;
        do {
            count++;
            String number = String.format("%03d", count);
            memberNumber = prefix + number;
        } while (memberRepository.existsByMemberNumber(memberNumber));

        log.info("Generated {} member number: {}", isAdmin ? "admin" : "member", memberNumber);
        return memberNumber;
    }

    public String getNextMemberNumber(boolean isAdmin) {
        String prefix = isAdmin ? "ADM" : "MB";
        long count = memberRepository.countByMemberNumberStartingWith(prefix);
        return prefix + String.format("%03d", count + 1);
    }
}