package com.sacco.banking.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FeeCalculationService {

    public BigDecimal calculateInternalTransferFee(BigDecimal amount) {
        // Free internal transfers
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateExternalTransferFee(BigDecimal amount) {
        // External transfer fee: 1% or minimum 50
        BigDecimal percentageFee = amount.multiply(new BigDecimal("0.01"));
        BigDecimal minimumFee = new BigDecimal("50");

        return percentageFee.max(minimumFee);
    }

    public BigDecimal calculateMpesaTransferFee(BigDecimal amount) {
        // M-Pesa transfer fees based on amount tiers
        if (amount.compareTo(new BigDecimal("100")) <= 0) {
            return BigDecimal.ZERO;
        } else if (amount.compareTo(new BigDecimal("500")) <= 0) {
            return new BigDecimal("7");
        } else if (amount.compareTo(new BigDecimal("1000")) <= 0) {
            return new BigDecimal("13");
        } else if (amount.compareTo(new BigDecimal("1500")) <= 0) {
            return new BigDecimal("23");
        } else if (amount.compareTo(new BigDecimal("2500")) <= 0) {
            return new BigDecimal("33");
        } else if (amount.compareTo(new BigDecimal("3500")) <= 0) {
            return new BigDecimal("53");
        } else if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            return new BigDecimal("57");
        } else if (amount.compareTo(new BigDecimal("7500")) <= 0) {
            return new BigDecimal("78");
        } else if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return new BigDecimal("90");
        } else {
            return new BigDecimal("105");
        }
    }
}