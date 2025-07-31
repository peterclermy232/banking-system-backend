package com.sacco.banking.repository;

import com.sacco.banking.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberNumber(String memberNumber);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdNumber(String idNumber);

    boolean existsByMemberNumber(String memberNumber);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);
    @Query("SELECT m FROM Member m WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.memberNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Member> findMembersWithSearch(@Param("search") String search, Pageable pageable);

    // Alternative method names that follow Spring Data JPA conventions
    Page<Member> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    long countByStatus(Member.MemberStatus memberStatus);
}