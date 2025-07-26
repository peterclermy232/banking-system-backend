package com.sacco.banking.repository;

import com.sacco.banking.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberNumber(String memberNumber);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);

    @Query("SELECT m FROM Member m WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(m.memberNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Member> findMembersWithSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    long countByStatus(Member.MemberStatus status);
}