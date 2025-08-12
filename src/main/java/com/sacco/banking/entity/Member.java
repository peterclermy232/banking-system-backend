package com.sacco.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_number", unique = true, nullable = false)
    private String memberNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    private LocalDateTime lastLoginDate;

    private String password;
    private String nationalId;
    private String occupation;
    private LocalDateTime dateJoined;

    @Column(name = "share_capital", nullable = false, precision = 19, scale = 2)
    private BigDecimal shareCapital;

    @Column(name = "savings_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal savingsBalance = BigDecimal.ZERO;

    @Column(name = "total_savings", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSavings = BigDecimal.ZERO;

    private int creditScore; // Make sure this field exists

    @Column(name = "monthly_income", nullable = false)
    private Integer monthlyIncome;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "member_roles",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public enum MemberStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        TERMINATED
    }

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (shareCapital == null) {
            shareCapital = BigDecimal.ZERO;
        }
        if (savingsBalance == null) {
            savingsBalance = BigDecimal.ZERO;
        }
        if (totalSavings == null) {
            totalSavings = BigDecimal.ZERO;
        }
        if (monthlyIncome == null) {
            monthlyIncome = 0;
        }
        if (status == null) {
            status = MemberStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

}