package com.sacco.banking.service;

import com.sacco.banking.dto.request.LoginRequest;
import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.AuthResponse;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.entity.Role;
import com.sacco.banking.enums.AccountStatus;
import com.sacco.banking.enums.AccountType;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.repository.RoleRepository;
import com.sacco.banking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final NotificationHelper notificationHelper;

    // Admin domain whitelist - only these domains can register as admin
    @Value("${app.admin.allowed-domains:admin.sacco.com,board.sacco.com}")
    private String[] allowedAdminDomains;

    // Secret admin registration code - should be environment variable in production
    @Value("${app.admin.registration-code:SACCO_ADMIN_2024}")
    private String adminRegistrationCode;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        // Find member by username (memberNumber) or email
        Member member = memberRepository.findByMemberNumber(loginRequest.getUsername())
                .or(() -> memberRepository.findByEmail(loginRequest.getUsername()))
                .orElseThrow(() -> new BadRequestException("Invalid username/email or password"));

        // Check if member is active
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            throw new BadRequestException("Account is " + member.getStatus().toString().toLowerCase() +
                    ". Please contact support.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        member.getMemberNumber(), // Always use memberNumber for authentication
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        // Update last login time
        member.setLastLoginDate(LocalDateTime.now());
        memberRepository.save(member);

        log.info("Member {} logged in successfully", member.getMemberNumber());

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .member(MemberResponse.fromEntity(member))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        return register(registerRequest, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest, String adminCode) {
        // Check if email already exists
        if (memberRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        // Check if national ID already exists
        if (memberRepository.existsByIdNumber(registerRequest.getNationalId())) {
            throw new BadRequestException("National ID already exists");
        }

        // Determine if this should be an admin registration
        boolean isAdminRegistration = isAdminEmail(registerRequest.getEmail());

        if (isAdminRegistration) {
            validateAdminRegistration(registerRequest.getEmail(), adminCode);
        }

        // Generate member number
        String memberNumber = generateMemberNumber(isAdminRegistration);

        Member member = new Member();
        member.setMemberNumber(memberNumber);
        member.setFirstName(registerRequest.getFirstName());
        member.setLastName(registerRequest.getLastName());
        member.setEmail(registerRequest.getEmail());
        member.setPhoneNumber(registerRequest.getPhoneNumber());
        member.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        member.setNationalId(registerRequest.getNationalId());
        member.setAddress(registerRequest.getAddress());
        member.setOccupation(registerRequest.getOccupation());
        member.setDateOfBirth(registerRequest.getDateOfBirth());
        member.setIdNumber(registerRequest.getIdNumber());
        member.setShareCapital(registerRequest.getShareCapital() != null ? registerRequest.getShareCapital() : BigDecimal.ZERO);
        member.setMonthlyIncome(registerRequest.getMonthlyIncome() != null ? registerRequest.getMonthlyIncome() : 0);
        member.setDateJoined(LocalDateTime.now());
        member.setLastLoginDate(LocalDateTime.now());
        member.setStatus(Member.MemberStatus.ACTIVE);
        member.setCreditScore(700);

        // Assign roles
        Set<Role> roles = new HashSet<>();
        if (isAdminRegistration) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);

            // Also add member role for basic functionalities
            Role memberRole = roleRepository.findByName(RoleName.ROLE_MEMBER)
                    .orElseThrow(() -> new RuntimeException("Member role not found"));
            roles.add(memberRole);
        } else {
            Role memberRole = roleRepository.findByName(RoleName.ROLE_MEMBER)
                    .orElseThrow(() -> new RuntimeException("Member role not found"));
            roles.add(memberRole);
        }
        member.setRoles(roles);

        Member savedMember = memberRepository.save(member);

        // Create default accounts (only for regular members, admins might not need all accounts)
        if (!isAdminRegistration) {
            createDefaultAccounts(savedMember);
        } else {
            createAdminAccounts(savedMember);
        }

        // Send welcome notification
        notificationHelper.notifyWelcome(savedMember.getMemberNumber(), savedMember.getFirstName());

        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        savedMember.getMemberNumber(),
                        registerRequest.getPassword()
                )
        );

        String jwt = tokenProvider.generateToken(authentication);

        log.info("New {} {} registered successfully",
                isAdminRegistration ? "admin" : "member", savedMember.getMemberNumber());

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .member(MemberResponse.fromEntity(savedMember))
                .build();
    }

    private boolean isAdminEmail(String email) {
        if (allowedAdminDomains == null || allowedAdminDomains.length == 0) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf("@") + 1);
        for (String allowedDomain : allowedAdminDomains) {
            if (domain.equalsIgnoreCase(allowedDomain.trim())) {
                return true;
            }
        }
        return false;
    }

    private void validateAdminRegistration(String email, String providedCode) {
        // Check if admin code is provided and matches
        if (providedCode == null || !adminRegistrationCode.equals(providedCode)) {
            throw new BadRequestException("Invalid admin registration code. Contact system administrator.");
        }

        // Additional security: Check if there are already too many admins
        long adminCount = memberRepository.count(); // You might want to create a specific method to count admins
        if (adminCount > 10) { // Configurable limit
            log.warn("Admin registration attempt with email {} when admin limit might be reached", email);
        }
    }

    private String generateMemberNumber(boolean isAdmin) {
        String prefix = isAdmin ? "ADM" : "MB";
        long count;

        if (isAdmin) {
            // Count existing admin members
            count = memberRepository.count(); // You might want to create a method to count only admins
        } else {
            // Count existing regular members
            count = memberRepository.count();
        }

        // Format: ADM001, ADM002... or MB001, MB002...
        String number = String.format("%03d", count + 1);
        String memberNumber = prefix + number;

        // Ensure uniqueness (in case of concurrent registrations)
        while (memberRepository.existsByMemberNumber(memberNumber)) {
            count++;
            number = String.format("%03d", count + 1);
            memberNumber = prefix + number;
        }

        return memberNumber;
    }

    private void createDefaultAccounts(Member member) {
        // Savings Account
        Account savingsAccount = createAccount(member, AccountType.SAVINGS, "SAV",
                new BigDecimal("1000"), new BigDecimal("0.05"));
        accountRepository.save(savingsAccount);
        notificationHelper.notifyAccountCreated(member.getMemberNumber(), "SAVINGS");

        // Current Account
        Account currentAccount = createAccount(member, AccountType.CURRENT, "CUR",
                BigDecimal.ZERO, BigDecimal.ZERO);
        accountRepository.save(currentAccount);
        notificationHelper.notifyAccountCreated(member.getMemberNumber(), "CURRENT");

        // Share Capital Account
        Account shareCapitalAccount = createAccount(member, AccountType.SHARE_CAPITAL, "SHR",
                new BigDecimal("5000"), new BigDecimal("0.08"));
        accountRepository.save(shareCapitalAccount);
        notificationHelper.notifyAccountCreated(member.getMemberNumber(), "SHARE_CAPITAL");

        log.info("Created default accounts for member {}", member.getMemberNumber());
    }

    private void createAdminAccounts(Member member) {
        // Admins get only basic accounts
        Account currentAccount = createAccount(member, AccountType.CURRENT, "CUR",
                BigDecimal.ZERO, BigDecimal.ZERO);
        accountRepository.save(currentAccount);
        notificationHelper.notifyAccountCreated(member.getMemberNumber(), "CURRENT");

        log.info("Created admin account for {}", member.getMemberNumber());
    }

    private Account createAccount(Member member, AccountType type, String prefix,
                                  BigDecimal minBalance, BigDecimal interestRate) {
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber(prefix));
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setMinimumBalance(minBalance);
        account.setInterestRate(interestRate);
        account.setMember(member);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedDate(LocalDateTime.now());
        account.setUpdatedDate(LocalDateTime.now());
        return account;
    }

    private String generateAccountNumber(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // Rest of the methods remain the same...
    @Transactional
    public void changePassword(String memberNumber, String oldPassword, String newPassword) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        member.setUpdatedDate(LocalDateTime.now());
        memberRepository.save(member);

        notificationHelper.notifyPasswordChanged(memberNumber);
        log.info("Password changed for member {}", memberNumber);
    }

    @Transactional
    public void resetPassword(String memberNumber, String newPassword) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new BadRequestException("Member not found"));

        member.setPassword(passwordEncoder.encode(newPassword));
        member.setUpdatedDate(LocalDateTime.now());
        memberRepository.save(member);

        notificationHelper.notifySecurityAlert(memberNumber,
                "Your password has been reset. If you didn't request this, please contact support immediately.");
        log.info("Password reset for member {}", memberNumber);
    }
}