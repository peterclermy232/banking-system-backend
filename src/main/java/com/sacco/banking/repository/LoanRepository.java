package com.sacco.banking.repository;

import com.sacco.banking.entity.Loan;
import com.sacco.banking.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByMemberOrderByCreatedAtDesc(Member member);
    Optional<Loan> findByIdAndMember(Long id, Member member);
    Optional<Loan> findByLoanNumber(String loanNumber);
    long countByMemberAndStatusIn(Member member, List<Loan.LoanStatus> statuses);
    List<Loan> findByStatus(Loan.LoanStatus status);
}