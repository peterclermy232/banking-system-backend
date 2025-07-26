package com.sacco.banking.service;

import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.dto.response.MemberStatsResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.exception.ResourceNotFoundException;
import com.sacco.banking.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Page<MemberResponse> getAllMembers(Pageable pageable, String search) {
        Page<Member> members = memberRepository.findMembersWithSearch(search, pageable);
        return members.map(MemberResponse::fromEntity);
    }

    public MemberStatsResponse getMemberStats() {
        long totalMembers = memberRepository.count();
        long activeMembers = memberRepository.countByStatus(Member.MemberStatus.ACTIVE);
        long inactiveMembers = memberRepository.countByStatus(Member.MemberStatus.INACTIVE);
        long suspendedMembers = memberRepository.countByStatus(Member.MemberStatus.SUSPENDED);

        return MemberStatsResponse.builder()
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .inactiveMembers(inactiveMembers)
                .suspendedMembers(suspendedMembers)
                .build();
    }

    public MemberResponse getMemberByNumber(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        return MemberResponse.fromEntity(member);
    }
}