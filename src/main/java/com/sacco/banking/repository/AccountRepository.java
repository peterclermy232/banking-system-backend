package com.sacco.banking.repository;

import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.enums.AccountStatus;
import com.sacco.banking.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Custom search method with proper @Query annotation
    @Query("SELECT a FROM Account a WHERE " +
            "LOWER(a.member.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.member.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.member.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.member.memberNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Account> findAccountsByMemberSearch(@Param("search") String search, Pageable pageable);

    boolean existsByMemberMemberNumberAndAccountType(String memberNumber, AccountType accountType);

    List<Account> findByMemberMemberNumberAndStatusOrderByCreatedDateDesc(String memberNumber,AccountStatus status);

    Optional<Account> findByIdAndMemberMemberNumber(Long id, String memberNumber);

    Optional<Account> findByAccountNumberAndMemberMemberNumber(String accountNumber, String memberNumber);

    List<Account> findByMemberMemberNumberAndStatus(String memberNumber, AccountStatus status);

    Page<Account> findAllByOrderByCreatedDateDesc(Pageable pageable);

    List<Account> findByStatus(AccountStatus status);

    List<Account> findByAccountTypeAndStatus(AccountType accountType, AccountStatus status);
    // ✅ This resolves: findByAccountNumber(...)
    Optional<Account> findByAccountNumber(String accountNumber);

    // ✅ This resolves: findFirstByMemberAndAccountType(...)
    Optional<Account> findFirstByMemberAndAccountType(Member member, AccountType accountType);
    List<Account> findByMember(Member member);
}