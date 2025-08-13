package com.sacco.banking.repository;

import com.sacco.banking.entity.Member;
import com.sacco.banking.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberNumber(String memberNumber);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.roles WHERE m.memberNumber = :memberNumber")
    Optional<Member> findByMemberNumberWithRoles(@Param("memberNumber") String memberNumber);

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

    long countByStatus(Member.MemberStatus memberStatus);

    @Query("SELECT COALESCE(SUM(m.totalSavings), 0) FROM Member m WHERE m.status != 'TERMINATED'")
    BigDecimal getTotalSavingsSum();

    // Count members with admin role and specific status
    @Query("SELECT COUNT(m) FROM Member m JOIN m.roles r WHERE r.name = :roleName AND m.status = :status")
    long countByRoleAndStatus(@Param("roleName") RoleName roleName, @Param("status") Member.MemberStatus status);

    // Find members by role
    @Query("SELECT m FROM Member m JOIN m.roles r WHERE r.name = :roleName")
    Page<Member> findByRole(@Param("roleName") RoleName roleName, Pageable pageable);

    // Count members by member number prefix
    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberNumber LIKE CONCAT(:prefix, '%')")
    long countByMemberNumberStartingWith(@Param("prefix") String prefix);

    // Find the last member number with specific prefix
    @Query("SELECT m.memberNumber FROM Member m WHERE m.memberNumber LIKE CONCAT(:prefix, '%') ORDER BY m.memberNumber DESC")
    Optional<String> findLastMemberNumberWithPrefix(@Param("prefix") String prefix);

    // Get admin statistics
    @Query("SELECT COUNT(m) FROM Member m JOIN m.roles r WHERE r.name = 'ROLE_ADMIN'")
    long countAdmins();

    @Query("SELECT COUNT(m) FROM Member m JOIN m.roles r WHERE r.name = 'ROLE_MEMBER' AND NOT EXISTS " +
            "(SELECT r2 FROM m.roles r2 WHERE r2.name = 'ROLE_ADMIN')")
    long countRegularMembers();
}