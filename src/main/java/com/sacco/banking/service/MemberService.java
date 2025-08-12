package com.sacco.banking.service;

import com.sacco.banking.dto.request.UpdateMemberRequest;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.dto.response.MemberStatsResponse;
import com.sacco.banking.entity.Member;
import com.sacco.banking.exception.ResourceNotFoundException;
import com.sacco.banking.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    public MemberResponse suspendMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        member.setStatus(Member.MemberStatus.SUSPENDED);
        memberRepository.save(member);

        return MemberResponse.fromEntity(member);
    }

    public MemberResponse activateMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        member.setStatus(Member.MemberStatus.ACTIVE);
        memberRepository.save(member);

        return MemberResponse.fromEntity(member);
    }

    public void deleteMember(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        // For soft delete, you can mark the member as TERMINATED
        member.setStatus(Member.MemberStatus.TERMINATED);
        memberRepository.save(member);
    }

    public BigDecimal getTotalSavings() {
        BigDecimal total = memberRepository.getTotalSavingsSum();
        return total != null ? total : BigDecimal.ZERO;
    }

    // New update methods
    public MemberResponse updateMember(String memberNumber, UpdateMemberRequest request) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        // Validate member can be updated
        validateMemberUpdate(member, request);

        // Validate email uniqueness if email is being changed
        if (request.getEmail() != null && !request.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
        }

        // Update all provided fields (PUT - full update)
        updateMemberFields(member, request);

        Member savedMember = memberRepository.save(member);
        return MemberResponse.fromEntity(savedMember);
    }

    public MemberResponse partialUpdateMember(String memberNumber, UpdateMemberRequest request) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with number: " + memberNumber));

        // Validate member can be updated
        validateMemberUpdate(member, request);

        // Validate email uniqueness if email is being changed
        if (request.getEmail() != null && !request.getEmail().equals(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
        }

        // Update only non-null fields (PATCH - partial update)
        updateMemberFieldsPartial(member, request);

        Member savedMember = memberRepository.save(member);
        return MemberResponse.fromEntity(savedMember);
    }

    private void updateMemberFields(Member member, UpdateMemberRequest request) {
        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            member.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getOccupation() != null) {
            member.setOccupation(request.getOccupation());
        }
        if (request.getShareCapital() != null) {
            member.setShareCapital(request.getShareCapital());
        }
        if(request.getMonthlyIncome() != null){
            member.setMonthlyIncome(request.getMonthlyIncome());
        }
        if(request.getIdNumber() != null){
            member.setIdNumber(request.getIdNumber());
        }
    }

    private void updateMemberFieldsPartial(Member member, UpdateMemberRequest request) {
        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            member.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            member.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getOccupation() != null) {
            member.setOccupation(request.getOccupation());
        }
        if (request.getShareCapital() != null && request.getShareCapital().compareTo(BigDecimal.ZERO) >= 0) {
            member.setShareCapital(request.getShareCapital());
        }
        if(request.getMonthlyIncome() != null){
            member.setMonthlyIncome(request.getMonthlyIncome());
        }
        if(request.getIdNumber() != null){
            member.setIdNumber(request.getIdNumber());
        }
    }

    private void validateMemberUpdate(Member member, UpdateMemberRequest request) {
        // Prevent updates to terminated members
        if (member.getStatus() == Member.MemberStatus.TERMINATED) {
            throw new IllegalStateException("Cannot update terminated member");
        }

        // Validate share capital constraints
        if (request.getShareCapital() != null && request.getShareCapital().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Share capital cannot be negative");
        }

        // Add any other business validation rules here
    }
}