package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


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
    private String idNumber;
    private String address;
    private String occupation;
    private LocalDateTime dateJoined;
    private Integer monthlyIncome;
    private LocalDate dateOfBirth;
    private List<String> roles;

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
                .idNumber(member.getIdNumber())
                .address(member.getAddress())
                .occupation(member.getOccupation())
                .dateJoined(member.getDateJoined())
                .monthlyIncome(member.getMonthlyIncome())
                .dateOfBirth(member.getDateOfBirth())
                .roles(
                        member.getRoles().stream()
                                .map(role -> role.getName().name()) // Converts enum to String
                                .toList()
                )
                .build();
    }
}