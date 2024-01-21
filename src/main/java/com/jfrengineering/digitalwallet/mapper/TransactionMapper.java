package com.jfrengineering.digitalwallet.mapper;

import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionMapper {

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Transaction transactionRequestToTransaction(TransactionRequest transactionRequest) {
        return Transaction.builder()
                .correlationId(transactionRequest.getCorrelationId())
                .customerId(transactionRequest.getCustomerId())
                .amount(transactionRequest.getAmount())
                .operation(transactionRequest.getOperation())
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
    }

    public static TransactionBalanceResponse transactionAndBalanceToTransactionBalanceResponse(
            UUID customerId, Transaction transaction, BigDecimal updatedBalance) {
        return new TransactionBalanceResponse(customerId, transactionToTransactionResponse(transaction), updatedBalance);
    }

    public static TransactionResponse transactionToTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getCorrelationId(),
                transaction.getAmount().setScale(2, RoundingMode.HALF_UP),
                transaction.getOperation(),
                DATE_TIME_FORMATTER.format(transaction.getCreatedAt().toLocalDateTime())
        );
    }
}
