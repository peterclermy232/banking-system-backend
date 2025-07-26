package com.sacco.banking.service;

import com.sacco.banking.dto.request.LoginRequest;
import com.sacco.banking.dto.request.RegisterRequest;
import com.sacco.banking.dto.response.AuthResponse;
import com.sacco.banking.dto.response.MemberResponse;
import com.sacco.banking.entity.Account;
import com.sacco.banking.entity.Member;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.MemberRepository;
import com.sacco.banking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getMemberNumber(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        Member member = memberRepository.findByMemberNumber(loginRequest.getMemberNumber())
                .orElseThrow(() -> new BadRequestException("Member not found"));

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .member(MemberResponse.fromEntity(member))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (memberRepository.findByMemberNumber(registerRequest.getMemberNumber()).isPresent()) {
            throw new BadRequestException("Member number already exists");
        }

        if (memberRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        Member member = new Member();
        member.setMemberNumber(registerRequest.getMemberNumber());
        member.setFirstName(registerRequest.getFirstName());
        member.setLastName(registerRequest.getLastName());
        member.setEmail(registerRequest.getEmail());
        member.setPhoneNumber(registerRequest.getPhoneNumber());
        member.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        member.setNationalId(registerRequest.getNationalId());
        member.setAddress(registerRequest.getAddress());
        member.setOccupation(registerRequest.getOccupation());
        member.setDateJoined(LocalDateTime.now());
        member.setStatus(Member.MemberStatus.ACTIVE);
        member.setCreditScore(700);

        Member savedMember = memberRepository.save(member);

        // Create default accounts
        createDefaultAccounts(savedMember);

        // Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getMemberNumber(),
                        registerRequest.getPassword()
                )
        );

        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .member(MemberResponse.fromEntity(savedMember))
                .build();
    }

    private void createDefaultAccounts(Member member) {
        // Savings Account
        Account savingsAccount = new Account();
        savingsAccount.setAccountNumber(generateAccountNumber("SAV"));
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccount.setMinimumBalance(new BigDecimal("1000"));
        savingsAccount.setInterestRate(new BigDecimal("0.05"));
        savingsAccount.setMember(member);
        savingsAccount.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(savingsAccount);

        // Current Account
        Account currentAccount = new Account();
        currentAccount.setAccountNumber(generateAccountNumber("CUR"));
        currentAccount.setAccountType(Account.AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.ZERO);
        currentAccount.setMinimumBalance(BigDecimal.ZERO);
        currentAccount.setInterestRate(BigDecimal.ZERO);
        currentAccount.setMember(member);
        currentAccount.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(currentAccount);

        // Share Capital Account
        Account shareCapitalAccount = new Account();
        shareCapitalAccount.setAccountNumber(generateAccountNumber("SHR"));
        shareCapitalAccount.setAccountType(Account.AccountType.SHARE_CAPITAL);
        shareCapitalAccount.setBalance(BigDecimal.ZERO);
        shareCapitalAccount.setMinimumBalance(new BigDecimal("5000"));
        shareCapitalAccount.setInterestRate(new BigDecimal("0.08"));
        shareCapitalAccount.setMember(member);
        shareCapitalAccount.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(shareCapitalAccount);
    }

    private String generateAccountNumber(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}