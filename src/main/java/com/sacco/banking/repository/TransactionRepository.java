package com.sacco.banking.repository;

import com.sacco.banking.entity.Transaction;
import com.sacco.banking.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount = :account OR t.toAccount = :account) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByAccount(@Param("account") Account account, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount IN :accounts OR t.toAccount IN :accounts) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByAccountsAndDateRange(
            @Param("accounts") List<Account> accounts,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}