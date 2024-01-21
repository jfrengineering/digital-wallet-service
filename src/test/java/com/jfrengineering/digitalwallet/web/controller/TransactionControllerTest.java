package com.jfrengineering.digitalwallet.web.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.service.TransactionService;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static com.jfrengineering.digitalwallet.util.TestUtils.createTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController underTest;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setUp() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(TransactionController.class)).addAppender(logWatcher);
    }

    @Test
    void getCustomerTransactions() {
        // Given
        UUID customerId = UUID.randomUUID();
        int pageNumber = 3;
        int pageSize = 20;

        // And
        TransactionsPageResponse transactionsPageResponse = mock(TransactionsPageResponse.class);
        when(transactionService.getTransactionsByCustomerId(customerId, pageNumber, pageSize)).thenReturn(transactionsPageResponse);

        // When
        ResponseEntity<TransactionsPageResponse> responseEntity =
                underTest.getCustomerTransactions(customerId, pageNumber, pageSize);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(transactionsPageResponse);

        // And
        verifyLogs(Level.INFO, "Received request to get customer transactions for customer with ID " + customerId);
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class)
    void createTransaction(Operation operation) {
        // Given
        UUID correlationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal transactionAmount = new BigDecimal("123.45");
        TransactionRequest transactionRequest = createTransactionRequest(correlationId, customerId, transactionAmount, operation);

        // And
        TransactionBalanceResponse transactionBalanceResponse = mock(TransactionBalanceResponse.class);
        when(transactionService.createTransaction(transactionRequest)).thenReturn(transactionBalanceResponse);

        // When
        ResponseEntity<TransactionBalanceResponse> responseEntity =
                underTest.createTransaction(transactionRequest);

        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseEntity.getBody()).isEqualTo(transactionBalanceResponse);

        // And
        verifyLogs(Level.INFO, "Received request to create transaction: " + transactionRequest);
    }

    private void verifyLogs(Level level, String message) {
        AssertionsForClassTypes.assertThat(logWatcher.list.size()).isEqualTo(1);
        AssertionsForClassTypes.assertThat(logWatcher.list.get(0))
                .extracting("level", "formattedMessage")
                .containsExactly(level, message);
    }
}