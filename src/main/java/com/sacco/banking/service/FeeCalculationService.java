package com.sacco.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class FeeCalculationService {

    // Fee configuration constants
    private static final BigDecimal INTERNAL_TRANSFER_FLAT_FEE = new BigDecimal("10.00");
    private static final BigDecimal EXTERNAL_TRANSFER_PERCENTAGE = new BigDecimal("0.015"); // 1.5%
    private static final BigDecimal EXTERNAL_TRANSFER_MIN_FEE = new BigDecimal("50.00");
    private static final BigDecimal EXTERNAL_TRANSFER_MAX_FEE = new BigDecimal("500.00");

    private static final BigDecimal MPESA_TRANSFER_PERCENTAGE = new BigDecimal("0.02"); // 2%
    private static final BigDecimal MPESA_MIN_FEE = new BigDecimal("20.00");
    private static final BigDecimal MPESA_MAX_FEE = new BigDecimal("300.00");

    private static final BigDecimal WITHDRAWAL_PERCENTAGE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal WITHDRAWAL_MIN_FEE = new BigDecimal("25.00");
    private static final BigDecimal WITHDRAWAL_MAX_FEE = new BigDecimal("200.00");

    private static final BigDecimal SAVINGS_DEPOSIT_FEE = BigDecimal.ZERO; // Free

    // Thresholds for fee tiers
    private static final BigDecimal TIER_1_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal TIER_2_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal TIER_3_THRESHOLD = new BigDecimal("100000");

    /**
     * Calculate fee for internal transfers (between member's own accounts)
     */
    public BigDecimal calculateInternalTransferFee(BigDecimal amount) {
        log.debug("Calculating internal transfer fee for amount: {}", amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // For internal transfers, use a flat fee structure with tiers
        BigDecimal fee;

        if (amount.compareTo(TIER_1_THRESHOLD) <= 0) {
            fee = INTERNAL_TRANSFER_FLAT_FEE; // KSH 10 for amounts <= 10,000
        } else if (amount.compareTo(TIER_2_THRESHOLD) <= 0) {
            fee = new BigDecimal("20.00"); // KSH 20 for amounts <= 50,000
        } else if (amount.compareTo(TIER_3_THRESHOLD) <= 0) {
            fee = new BigDecimal("30.00"); // KSH 30 for amounts <= 100,000
        } else {
            fee = new BigDecimal("50.00"); // KSH 50 for amounts > 100,000
        }

        log.debug("Internal transfer fee calculated: {} for amount: {}", fee, amount);
        return fee;
    }

    /**
     * Calculate fee for external transfers (to other banks)
     */
    public BigDecimal calculateExternalTransferFee(BigDecimal amount) {
        log.debug("Calculating external transfer fee for amount: {}", amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate percentage-based fee
        BigDecimal percentageFee = amount.multiply(EXTERNAL_TRANSFER_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);

        // Apply min and max limits
        BigDecimal fee = percentageFee;
        if (fee.compareTo(EXTERNAL_TRANSFER_MIN_FEE) < 0) {
            fee = EXTERNAL_TRANSFER_MIN_FEE;
        } else if (fee.compareTo(EXTERNAL_TRANSFER_MAX_FEE) > 0) {
            fee = EXTERNAL_TRANSFER_MAX_FEE;
        }

        log.debug("External transfer fee calculated: {} for amount: {}", fee, amount);
        return fee;
    }

    /**
     * Calculate fee for M-Pesa transfers
     */
    public BigDecimal calculateMpesaTransferFee(BigDecimal amount) {
        log.debug("Calculating M-Pesa transfer fee for amount: {}", amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // M-Pesa has tiered pricing structure similar to official M-Pesa rates
        BigDecimal fee = calculateTieredMpesaFee(amount);

        log.debug("M-Pesa transfer fee calculated: {} for amount: {}", fee, amount);
        return fee;
    }

    /**
     * Calculate withdrawal fee
     */
    public BigDecimal calculateWithdrawalFee(BigDecimal amount) {
        log.debug("Calculating withdrawal fee for amount: {}", amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate percentage-based fee
        BigDecimal percentageFee = amount.multiply(WITHDRAWAL_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);

        // Apply min and max limits
        BigDecimal fee = percentageFee;
        if (fee.compareTo(WITHDRAWAL_MIN_FEE) < 0) {
            fee = WITHDRAWAL_MIN_FEE;
        } else if (fee.compareTo(WITHDRAWAL_MAX_FEE) > 0) {
            fee = WITHDRAWAL_MAX_FEE;
        }

        log.debug("Withdrawal fee calculated: {} for amount: {}", fee, amount);
        return fee;
    }

    /**
     * Calculate fee for savings deposits (usually free)
     */
    public BigDecimal calculateSavingsDepositFee(BigDecimal amount) {
        log.debug("Calculating savings deposit fee for amount: {}", amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Savings deposits are typically free to encourage saving
        log.debug("Savings deposit fee: {} (free)", SAVINGS_DEPOSIT_FEE);
        return SAVINGS_DEPOSIT_FEE;
    }

    /**
     * Calculate tiered M-Pesa fees similar to official M-Pesa structure
     */
    private BigDecimal calculateTieredMpesaFee(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("49")) <= 0) {
            return BigDecimal.ZERO; // Free for very small amounts
        } else if (amount.compareTo(new BigDecimal("100")) <= 0) {
            return new BigDecimal("1.00");
        } else if (amount.compareTo(new BigDecimal("500")) <= 0) {
            return new BigDecimal("5.00");
        } else if (amount.compareTo(new BigDecimal("1000")) <= 0) {
            return new BigDecimal("10.00");
        } else if (amount.compareTo(new BigDecimal("1500")) <= 0) {
            return new BigDecimal("13.00");
        } else if (amount.compareTo(new BigDecimal("2500")) <= 0) {
            return new BigDecimal("20.00");
        } else if (amount.compareTo(new BigDecimal("3500")) <= 0) {
            return new BigDecimal("25.00");
        } else if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            return new BigDecimal("30.00");
        } else if (amount.compareTo(new BigDecimal("7500")) <= 0) {
            return new BigDecimal("40.00");
        } else if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return new BigDecimal("45.00");
        } else if (amount.compareTo(new BigDecimal("15000")) <= 0) {
            return new BigDecimal("50.00");
        } else if (amount.compareTo(new BigDecimal("20000")) <= 0) {
            return new BigDecimal("55.00");
        } else if (amount.compareTo(new BigDecimal("25000")) <= 0) {
            return new BigDecimal("60.00");
        } else if (amount.compareTo(new BigDecimal("35000")) <= 0) {
            return new BigDecimal("75.00");
        } else if (amount.compareTo(new BigDecimal("50000")) <= 0) {
            return new BigDecimal("90.00");
        } else if (amount.compareTo(new BigDecimal("70000")) <= 0) {
            return new BigDecimal("110.00");
        } else {
            // For amounts above 70,000, use percentage with max limit
            BigDecimal percentageFee = amount.multiply(MPESA_TRANSFER_PERCENTAGE)
                    .setScale(2, RoundingMode.HALF_UP);

            if (percentageFee.compareTo(MPESA_MIN_FEE) < 0) {
                percentageFee = MPESA_MIN_FEE;
            } else if (percentageFee.compareTo(MPESA_MAX_FEE) > 0) {
                percentageFee = MPESA_MAX_FEE;
            }

            return percentageFee;
        }
    }
}

