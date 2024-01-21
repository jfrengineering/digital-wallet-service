package com.jfrengineering.digitalwallet.util;

import com.jfrengineering.digitalwallet.domain.Balance;
import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestUtils {

    public static final UUID CUSTOMER_ID_1 = UUID.randomUUID();
    public static final UUID CUSTOMER_ID_2 = UUID.randomUUID();
    public static final BigDecimal BALANCE_CUSTOMER_1 = new BigDecimal("12345.67");
    public static final BigDecimal BALANCE_CUSTOMER_2 = new BigDecimal("987.65");
    public static final UUID CORRELATION_ID_A = UUID.randomUUID();
    public static final UUID CORRELATION_ID_B = UUID.randomUUID();
    public static final BigDecimal TRANSACTION_AMOUNT =  new BigDecimal("123.45");

    public static Balance createBalance(UUID customerId, BigDecimal balanceAmount) {
        return createBalance(customerId, balanceAmount, null, null);
    }

    public static Balance createBalance(UUID customerId, BigDecimal balanceAmount,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return Balance.builder()
                .customerId(customerId)
                .balanceAmount(balanceAmount)
                .createdAt(createdAt == null ? null : Timestamp.valueOf(createdAt))
                .updatedAt(updatedAt == null ? null : Timestamp.valueOf(updatedAt))
                .build();
    }

    public static Transaction createTransaction(UUID correlationId, UUID customerId, BigDecimal transactionAmount,
                                                Operation operation) {
        return createTransaction(correlationId, customerId, transactionAmount, operation, null);
    }

    public static Transaction createTransaction(UUID correlationId, UUID customerId, BigDecimal transactionAmount,
                                                Operation operation, LocalDateTime createdAt) {
        return Transaction.builder()
                .correlationId(correlationId)
                .customerId(customerId)
                .amount(transactionAmount)
                .operation(operation)
                .createdAt(createdAt == null ? null : Timestamp.valueOf(createdAt))
                .build();
    }

    public static TransactionRequest createTransactionRequest(UUID correlationId, UUID customerId, BigDecimal transactionAmount,
                                                              Operation operation) {
        return TransactionRequest.builder()
                .correlationId(correlationId)
                .customerId(customerId)
                .amount(transactionAmount)
                .operation(operation)
                .build();
    }
}
