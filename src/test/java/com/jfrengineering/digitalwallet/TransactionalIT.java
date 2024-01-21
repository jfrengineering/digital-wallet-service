package com.jfrengineering.digitalwallet;

import com.jfrengineering.digitalwallet.domain.Balance;
import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.repository.BalanceRepository;
import com.jfrengineering.digitalwallet.repository.TransactionRepository;
import com.jfrengineering.digitalwallet.service.TransactionService;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_2;
import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_B;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_2;
import static com.jfrengineering.digitalwallet.util.TestUtils.createBalance;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionalIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        Balance balanceCustomer1 = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1);
        Balance balanceCustomer2 = createBalance(CUSTOMER_ID_2, BALANCE_CUSTOMER_2);
        balanceRepository.saveAll(List.of(balanceCustomer1, balanceCustomer2));
    }

    @AfterEach
    void tearDown() {
        balanceRepository.deleteAll();
        transactionRepository.deleteAll();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", transactionRepository);
    }

    @Test
    void createTransaction_rollbacksWithoutUpdatingBalance_ifSavingTransactionFails() {
        // Given a customer has a certain balance
        BigDecimal initialBalanceAmount = balanceRepository.findById(CUSTOMER_ID_1).get().getBalanceAmount();

        // And a transaction request
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_B, CUSTOMER_ID_1, BigDecimal.TEN,
                Operation.ADD);

        // And
        TransactionRepository mockTransactionRepository = mock(TransactionRepository.class);
        when(mockTransactionRepository.insert(any(Transaction.class))).thenThrow(new PersistenceException("Simulated exception"));
        ReflectionTestUtils.setField(transactionService, "transactionRepository", mockTransactionRepository);

        // When
        assertThrows(PersistenceException.class, () -> transactionService.createTransaction(transactionRequest));

        // Then no commits to database, because rollback ('@Transactional' method)
        BigDecimal finalBalanceAmount = balanceRepository.findById(CUSTOMER_ID_1).get().getBalanceAmount();
        assertThat(finalBalanceAmount).usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(initialBalanceAmount);
        Page<Transaction> customerTransactions = transactionRepository.findByCustomerId(CUSTOMER_ID_1, PageRequest.of(0, 5));
        assertThat(customerTransactions).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class)
    void createDuplicateTransaction_onlyProcessFirstTransaction_andEntityExistsExceptionForSecondTransaction(Operation operation) {
        // Given a customer with a certain balance and no previous transactions
        UUID customerId = UUID.randomUUID();
        BigDecimal initialBalanceAmount = new BigDecimal("100");
        balanceRepository.save(createBalance(customerId, initialBalanceAmount));

        // And a first transaction request
        UUID correlationId = UUID.randomUUID();
        BigDecimal transactionAmount = BigDecimal.TEN;
        TransactionRequest transactionRequest = createTransactionRequest(correlationId, customerId, transactionAmount,
                operation);

        // And the service is in the middle of the execution of this first transaction request
        TransactionRepository mockTransactionRepository = mock(TransactionRepository.class);
        when(mockTransactionRepository.insert(any(Transaction.class))).thenCallRealMethod();
        when(mockTransactionRepository.existsById(any(UUID.class))).then(invocationOnMock ->
                transactionRepository.existsById(invocationOnMock.getArgument(0)));
        when(mockTransactionRepository.save(any(Transaction.class))).then(invocationOnMock -> {
            Transaction savedTransaction = transactionRepository.save(invocationOnMock.getArgument(0));
            Thread.sleep(1000L); // simulate delay during first transaction to have second one executing in parallel
            return savedTransaction;
        });
        ReflectionTestUtils.setField(transactionService, "transactionRepository", mockTransactionRepository);

        transactionService.createTransaction(transactionRequest);

        // When a second transaction request (with same 'correlationId') comes in parallel
        assertThrows(EntityExistsException.class, () -> transactionService.createTransaction(transactionRequest));

        // Then only the first transaction was applied, whereas the second one was rolled-back
        BigDecimal expectedFinalBalanceAmount = operation == Operation.ADD
                ? initialBalanceAmount.add(transactionAmount)
                : initialBalanceAmount.subtract(transactionAmount);
        BigDecimal finalBalanceAmount = balanceRepository.findById(customerId).get().getBalanceAmount();
        assertThat(finalBalanceAmount).usingComparator(BigDecimal::compareTo).isEqualTo(expectedFinalBalanceAmount);
        Page<Transaction> customerTransactions = transactionRepository.findByCustomerId(customerId, PageRequest.of(0, 5));
        assertThat(customerTransactions).hasSize(1);
    }
}
