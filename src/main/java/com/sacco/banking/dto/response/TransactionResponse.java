package com.sacco.banking.dto.response;

import com.sacco.banking.entity.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String description;
    private String reference;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String status;
    private String mpesaReceiptNumber;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .description(transaction.getDescription())
                .reference(transaction.getReference())
                .fromAccountNumber(transaction.getFromAccount() != null ?
                        transaction.getFromAccount().getAccountNumber() : null)
                .toAccountNumber(transaction.getToAccount() != null ?
                        transaction.getToAccount().getAccountNumber() : null)
                .status(transaction.getStatus().name())
                .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}