package com.jfrengineering.digitalwallet.mapper;

import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionResponse;
import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.jfrengineering.digitalwallet.mapper.TransactionMapper.DATE_TIME_FORMATTER;
import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_A;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.TRANSACTION_AMOUNT;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransaction;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    @Test
    void transactionRequestToTransaction() {
        // Given
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1,
                TRANSACTION_AMOUNT, Operation.ADD);

        // When
        Transaction actual = TransactionMapper.transactionRequestToTransaction(transactionRequest);

        // Then
        assertThat(actual)
                .extracting(
                        "correlationId",
                        "customerId",
                        "amount",
                        "operation")
                .containsExactly(
                        CORRELATION_ID_A,
                        CUSTOMER_ID_1,
                        TRANSACTION_AMOUNT,
                        Operation.ADD
                );
        assertThat(actual.getCreatedAt().toLocalDateTime()).isCloseTo(LocalDateTime.now(),
                        new TemporalUnitLessThanOffset(5, ChronoUnit.SECONDS));
    }

    @Test
    void transactionAndBalanceToTransactionBalanceResponse() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Transaction transaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.WITHDRAW, createdAt);

        // When
        TransactionBalanceResponse actual =
                TransactionMapper.transactionAndBalanceToTransactionBalanceResponse(CUSTOMER_ID_1, transaction,
                        BALANCE_CUSTOMER_1);

        // Then
        assertThat(actual.getCustomerId()).isEqualTo(CUSTOMER_ID_1);
        assertThat(actual.getTransaction())
                .extracting(
                        "correlationId",
                        "amount",
                        "operation")
                .containsExactly(
                        CORRELATION_ID_A,
                        TRANSACTION_AMOUNT,
                        Operation.WITHDRAW
                );
        LocalDateTime actualCreatedAt = LocalDateTime.parse(actual.getTransaction().getCreatedAt(), DATE_TIME_FORMATTER);
        assertThat(actualCreatedAt).isCloseTo(createdAt, new TemporalUnitLessThanOffset(1, ChronoUnit.SECONDS));
        assertThat(actual.getUpdatedBalance()).isEqualTo(BALANCE_CUSTOMER_1);
    }

    @Test
    void transactionToTransactionResponse() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Transaction transaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD, createdAt);

        // When
        TransactionResponse actual = TransactionMapper.transactionToTransactionResponse(transaction);

        // Then
        assertThat(actual)
                .extracting(
                        "correlationId",
                        "amount",
                        "operation")
                .containsExactly(
                        CORRELATION_ID_A,
                        TRANSACTION_AMOUNT,
                        Operation.ADD
                );
        LocalDateTime actualCreatedAt = LocalDateTime.parse(actual.getCreatedAt(), DATE_TIME_FORMATTER);
        assertThat(actualCreatedAt).isCloseTo(createdAt, new TemporalUnitLessThanOffset(1, ChronoUnit.SECONDS));
    }

}