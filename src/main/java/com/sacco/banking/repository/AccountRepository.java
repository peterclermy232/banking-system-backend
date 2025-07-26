package com.sacco.banking.repository;

import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByMember(Member member);

    // Returns all accounts of a specific type for a member
    List<Account> findByMemberAndAccountType(Member member, Account.AccountType accountType);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.member = :member")
    BigDecimal getTotalBalanceByMember(@Param("member") Member member);

    // Returns the first account of a specific type for a member (if exists)
    @Query("SELECT a FROM Account a WHERE a.member = :member AND a.accountType = :accountType")
    Optional<Account> findFirstByMemberAndAccountType(@Param("member") Member member,
                                                      @Param("accountType") Account.AccountType accountType);
}