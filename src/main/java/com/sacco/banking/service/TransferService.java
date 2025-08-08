package com.sacco.banking.service;

import com.sacco.banking.dto.request.TransferRequest;
import com.sacco.banking.dto.response.TransactionResponse;
import com.sacco.banking.entity.*;
import com.sacco.banking.enums.TransactionStatus;
import com.sacco.banking.enums.TransactionType;
import com.sacco.banking.exception.BadRequestException;
import com.sacco.banking.exception.InsufficientFundsException;
import com.sacco.banking.repository.AccountRepository;
import com.sacco.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MpesaService mpesaService;
    private final FeeCalculationService feeCalculationService;

    @Transactional
    public TransactionResponse processInternalTransfer(TransferRequest request, Member member) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new BadRequestException("From account not found"));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new BadRequestException("To account not found"));

        // Verify ownership of from account
        if (!fromAccount.getMember().getId().equals(member.getId())) {
            throw new BadRequestException("You don't own the source account");
        }

        // Check balance
        BigDecimal fee = feeCalculationService.calculateInternalTransferFee(request.getAmount());
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(TransactionType.TRANSFER_INTERNAL);
        transaction.setAmount(request.getAmount());
        transaction.setFee(fee);
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setStatus(TransactionStatus.COMPLETED);

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // Save changes
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional
    public TransactionResponse processExternalTransfer(TransferRequest request, Member member) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new BadRequestException("From account not found"));

        // Verify ownership
        if (!fromAccount.getMember().getId().equals(member.getId())) {
            throw new BadRequestException("You don't own the source account");
        }

        // Calculate fee
        BigDecimal fee = feeCalculationService.calculateExternalTransferFee(request.getAmount());
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(TransactionType.TRANSFER_EXTERNAL);
        transaction.setAmount(request.getAmount());
        transaction.setFee(fee);
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setFromAccount(fromAccount);
        transaction.setExternalReference(request.getToAccountNumber());
        transaction.setStatus(TransactionStatus.PROCESSING);

        // Update balance
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        accountRepository.save(fromAccount);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // TODO: Integrate with external banking API
        // For now, mark as completed
        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(savedTransaction);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Transactional
    public TransactionResponse processMpesaTransfer(TransferRequest request, Member member) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new BadRequestException("From account not found"));

        // Verify ownership
        if (!fromAccount.getMember().getId().equals(member.getId())) {
            throw new BadRequestException("You don't own the source account");
        }

        // Calculate fee
        BigDecimal fee = feeCalculationService.calculateMpesaTransferFee(request.getAmount());
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(TransactionType.MPESA_WITHDRAWAL);
        transaction.setAmount(request.getAmount());
        transaction.setFee(fee);
        transaction.setDescription(request.getDescription());
        transaction.setReference(request.getReference());
        transaction.setFromAccount(fromAccount);
        transaction.setExternalReference(request.getToAccountNumber()); // M-Pesa number
        transaction.setStatus(TransactionStatus.PROCESSING);

        // Update balance
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        accountRepository.save(fromAccount);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Process M-Pesa transfer
        try {
            String mpesaReceiptNumber = mpesaService.sendMoney(
                    request.getToAccountNumber(),
                    request.getAmount(),
                    request.getDescription()
            );

            savedTransaction.setMpesaReceiptNumber(mpesaReceiptNumber);
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
        } catch (Exception e) {
            // Reverse the transaction
            fromAccount.setBalance(fromAccount.getBalance().add(totalDebit));
            accountRepository.save(fromAccount);

            savedTransaction.setStatus(TransactionStatus.FAILED);
        }

        transactionRepository.save(savedTransaction);
        return TransactionResponse.fromEntity(savedTransaction);
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}