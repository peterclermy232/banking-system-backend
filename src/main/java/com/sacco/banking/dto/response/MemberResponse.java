package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {
    private Long id;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private BigDecimal shareCapital;
    private String status;
    private Integer creditScore;
    private String nationalId;
    private String address;
    private String occupation;
    private LocalDateTime dateJoined;

    public static MemberResponse fromEntity(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .shareCapital(member.getShareCapital())
                .status(member.getStatus().name())
                .creditScore(member.getCreditScore())
                .nationalId(member.getNationalId())
                .address(member.getAddress())
                .occupation(member.getOccupation())
                .dateJoined(member.getDateJoined())
                .build();
    }
}