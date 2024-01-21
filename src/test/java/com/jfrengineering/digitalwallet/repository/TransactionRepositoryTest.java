package com.jfrengineering.digitalwallet.repository;

import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_A;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.TRANSACTION_AMOUNT;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryTest {

    @Spy
    private TransactionRepository transactionRepository;

    @Test
    void insert_persistsTransaction_ifNotRepeatedCorrelationId() {
        // Given
        Transaction transaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT, Operation.ADD);

        // And
        when(transactionRepository.existsById(CORRELATION_ID_A)).thenReturn(false);

        // When
        transactionRepository.insert(transaction);

        // Then
        verify(transactionRepository).save(transaction);
    }

    @Test
    void insert_throwsEntityExistsException_ifRepeatedCorrelationId() {
        // Given
        Transaction transaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT, Operation.ADD);

        // And
        when(transactionRepository.existsById(CORRELATION_ID_A)).thenReturn(true);

        // When-Then
        EntityExistsException actualException = assertThrows(EntityExistsException.class,
                () -> transactionRepository.insert(transaction));
        assertThat(actualException.getMessage()).isEqualTo("Transaction with the same correlationId already exists");
    }
}