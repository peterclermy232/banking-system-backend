package com.sacco.banking.service;

import com.sacco.banking.dto.request.LoanApplicationRequest;
import com.sacco.banking.dto.request.LoanCalculationRequest;
import com.sacco.banking.dto.response.LoanResponse;
import com.sacco.banking.dto.response.LoanCalculationResponse;
import com.sacco.banking.entity.Loan;
import com.sacco.banking.entity.Member;
import com.sacco.banking.enums.LoanStatus;
import com.sacco.banking.enums.LoanType;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;

    public List<LoanResponse> getMemberLoans(Member member) {
        List<Loan> loans = loanRepository.findByMemberOrderByCreatedAtDesc(member);
        return loans.stream()
                .map(LoanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request, Member member) {
        // Check member eligibility
        if (member.getCreditScore() < 600) {
            throw new BadRequestException("Credit score too low for loan approval");
        }

        // Check for existing active loans
        long activeLoanCount = loanRepository.countByMemberAndStatusIn(member,
                List.of(LoanStatus.ACTIVE, LoanStatus.DISBURSED));

        if (activeLoanCount >= 3) {
            throw new BadRequestException("Maximum number of active loans reached");
        }

        // Create loan application
        Loan loan = new Loan();
        loan.setLoanNumber(generateLoanNumber());
        loan.setLoanType(LoanType.valueOf(request.getLoanType()));
        loan.setPrincipalAmount(request.getAmount());
        loan.setCurrentBalance(request.getAmount());
        loan.setInterestRate(getLoanInterestRate(request.getLoanType(), request.getAmount()));
        loan.setTermMonths(request.getTermMonths());
        loan.setRemainingMonths(request.getTermMonths());
        loan.setPurpose(request.getPurpose());
        loan.setMember(member);
        loan.setApplicationDate(LocalDateTime.now());
        loan.setStatus(LoanStatus.PENDING);

        // Calculate monthly payment
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.getAmount(),
                loan.getInterestRate(),
                request.getTermMonths()
        );
        loan.setMonthlyPayment(monthlyPayment);

        Loan savedLoan = loanRepository.save(loan);
        return LoanResponse.fromEntity(savedLoan);
    }

    public LoanCalculationResponse calculateLoan(LoanCalculationRequest request) {
        BigDecimal interestRate = getLoanInterestRate(request.getLoanType(), request.getAmount());
        BigDecimal monthlyPayment = calculateMonthlyPayment(request.getAmount(), interestRate, request.getTermMonths());
        BigDecimal totalPayment = monthlyPayment.multiply(BigDecimal.valueOf(request.getTermMonths()));
        BigDecimal totalInterest = totalPayment.subtract(request.getAmount());

        return LoanCalculationResponse.builder()
                .principalAmount(request.getAmount())
                .interestRate(interestRate)
                .termMonths(request.getTermMonths())
                .monthlyPayment(monthlyPayment)
                .totalPayment(totalPayment)
                .totalInterest(totalInterest)
                .build();
    }

    public LoanResponse getLoanDetails(Long loanId, Member member) {
        Loan loan = loanRepository.findByIdAndMember(loanId, member)
                .orElseThrow(() -> new BadRequestException("Loan not found"));

        return LoanResponse.fromEntity(loan);
    }

    private BigDecimal getLoanInterestRate(String loanType, BigDecimal amount) {
        // Interest rates based on loan type and amount
        return switch (loanType) {
            case "PERSONAL" -> new BigDecimal("0.15"); // 15%
            case "BUSINESS" -> new BigDecimal("0.12"); // 12%
            case "EMERGENCY" -> new BigDecimal("0.18"); // 18%
            case "ASSET_FINANCING" -> new BigDecimal("0.14"); // 14%
            case "EDUCATION" -> new BigDecimal("0.10"); // 10%
            default -> new BigDecimal("0.15");
        };
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusR.pow(termMonths));
        BigDecimal denominator = onePlusR.pow(termMonths).subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String generateLoanNumber() {
        return "LOAN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}