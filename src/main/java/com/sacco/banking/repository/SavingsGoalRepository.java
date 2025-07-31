package com.sacco.banking.repository;

import com.sacco.banking.entity.SavingsGoal;
import com.sacco.banking.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByMemberOrderByCreatedAtDesc(Member member);
    Optional<SavingsGoal> findByIdAndMember(Long id, Member member);
    List<SavingsGoal> findByStatus(SavingsGoal.GoalStatus status);

    List<SavingsGoal> findByMemberAndStatus(Member member, SavingsGoal.GoalStatus goalStatus);

    List<SavingsGoal> findByStatusAndTargetDateBetween(SavingsGoal.GoalStatus goalStatus, LocalDateTime now, LocalDateTime oneMonthFromNow);
}