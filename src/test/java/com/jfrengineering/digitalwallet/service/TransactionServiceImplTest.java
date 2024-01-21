package com.jfrengineering.digitalwallet.service;

import com.jfrengineering.digitalwallet.domain.Balance;
import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.mapper.TransactionMapper;
import com.jfrengineering.digitalwallet.repository.BalanceRepository;
import com.jfrengineering.digitalwallet.repository.TransactionRepository;
import com.jfrengineering.digitalwallet.web.exception.UnacceptedTransactionAmountException;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.jfrengineering.digitalwallet.service.TransactionServiceImpl.TRANSACTION_SORTING_FIELD;
import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_A;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_2;
import static com.jfrengineering.digitalwallet.util.TestUtils.TRANSACTION_AMOUNT;
import static com.jfrengineering.digitalwallet.util.TestUtils.createBalance;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransaction;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private CustomerCacheService customerCacheService;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl underTest;

    @Captor
    private ArgumentCaptor<Balance> balanceCaptor;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(customerCacheService.customerBalanceExists(argThat(id ->
                Set.of(CUSTOMER_ID_1, CUSTOMER_ID_2).contains(id)))).thenReturn(true);
    }

    @Test
    void getTransactionsByCustomerId_rethrowsPersistenceException() {
        // Given
        int page = 2;
        int size = 3;
        Sort sort = Sort.by(TRANSACTION_SORTING_FIELD).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        when(transactionRepository.findByCustomerId(CUSTOMER_ID_1, pageRequest))
                .thenThrow(new PersistenceException());

        // When
        assertThrows(PersistenceException.class, () -> underTest.getTransactionsByCustomerId(CUSTOMER_ID_1, page, size));
    }

    @Test
    void getTransactionsByCustomerId_throwsEntityNotFoundException_ifCustomerIdNotFound() {
        // Given
        int page = 2;
        int size = 3;

        // And
        UUID nonExistingCustomerId = UUID.randomUUID();
        when(customerCacheService.customerBalanceExists(nonExistingCustomerId)).thenReturn(false);

        // When-Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> underTest.getTransactionsByCustomerId(nonExistingCustomerId, page, size));
        assertThat(exception.getMessage()).isEqualTo(String.format("Non existing customer with ID '%s'", nonExistingCustomerId));
    }

    @Test
    void getTransactionsByCustomerId_returnsTransactions() {
        // Given
        int page = 2;
        int size = 3;
        Sort sort = Sort.by(TRANSACTION_SORTING_FIELD).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        List<Transaction> savedTransactions = List.of(
                createTransaction(UUID.randomUUID(), CUSTOMER_ID_1, new BigDecimal("10000"), Operation.ADD, LocalDateTime.now().minusDays(1)),
                createTransaction(UUID.randomUUID(), CUSTOMER_ID_1, new BigDecimal("5000"), Operation.WITHDRAW, LocalDateTime.now().minusDays(2)),
                createTransaction(UUID.randomUUID(), CUSTOMER_ID_1, new BigDecimal("10"), Operation.ADD, LocalDateTime.now().minusDays(3))
        );

        when(transactionRepository.findByCustomerId(CUSTOMER_ID_1, pageRequest))
                .thenReturn(new PageImpl<>(savedTransactions, pageRequest, savedTransactions.size()));

        // When
        TransactionsPageResponse actual = underTest.getTransactionsByCustomerId(CUSTOMER_ID_1, page, size);

        // Then
        assertThat(actual.getPageable())
                .extracting(
                        "pageNumber",
                        "pageSize",
                        "sort")
                .containsExactly(
                        page,
                        size,
                        sort
                );
        assertThat(actual.getContent())
                .isEqualTo(List.of(
                        TransactionMapper.transactionToTransactionResponse(savedTransactions.get(0)),
                        TransactionMapper.transactionToTransactionResponse(savedTransactions.get(1)),
                        TransactionMapper.transactionToTransactionResponse(savedTransactions.get(2)))
                );
    }

    @Test
    void createTransaction_rethrowsPersistenceException() {
        // Given
        Balance savedBalance = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD);

        // And
        Transaction savedTransaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD, LocalDateTime.now());
        when(transactionRepository.insert(any(Transaction.class))).thenThrow(new PersistenceException());

        // When-Then
        assertThrows(PersistenceException.class, () -> underTest.createTransaction(transactionRequest));
    }

    @Test
    void createTransaction_rethrowsEntityExistsException() {
        // Given
        Balance savedBalance = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD);

        // And
        when(transactionRepository.insert(any(Transaction.class))).thenCallRealMethod();
        when(transactionRepository.existsById(CORRELATION_ID_A)).thenReturn(true);

        // When-Then
        EntityExistsException actualException = assertThrows(EntityExistsException.class,
                () -> underTest.createTransaction(transactionRequest));
        assertThat(actualException.getMessage()).isEqualTo("Transaction with the same correlationId already exists");
    }

    @Test
    void createTransaction_throwsEntityNotFoundException_ifCustomerIdNotFound() {
        // Given
        UUID nonExistingCustomerId = UUID.randomUUID();
        when(customerCacheService.customerBalanceExists(nonExistingCustomerId)).thenReturn(false);

        // And
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, nonExistingCustomerId,
                TRANSACTION_AMOUNT, Operation.ADD);

        // When-Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> underTest.createTransaction(transactionRequest));
        assertThat(exception.getMessage()).isEqualTo(String.format("Non existing customer with ID '%s'", nonExistingCustomerId));
    }

    @Test
    void createTransaction_throwsException_ifBalanceForCustomerDoesNotExist() {
        // Given
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.empty());

        // And
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD);

        // When-Then
        assertThrows(NoSuchElementException.class, () -> underTest.createTransaction(transactionRequest));
        verify(balanceRepository, times(0)).save(any(Balance.class));
        verifyNoInteractions(transactionRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = { "9.99", "10000.01" })
    void createCreditTransaction_isRejected_ifCreditedAmountOutOfAcceptedRange(String amountStr) {
        // Given
        Balance savedBalance = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        BigDecimal creditAmount = new BigDecimal(amountStr);
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, creditAmount,
                Operation.ADD);

        // When-Then
        UnacceptedTransactionAmountException actualException = assertThrows(UnacceptedTransactionAmountException.class,
                () -> underTest.createTransaction(transactionRequest));
        String expectedMessage = creditAmount.compareTo(BigDecimal.TEN) < 0
                ? "Minimum accepted Credit Amount is £10.00"
                : "Maximum accepted Credit Amount is £10,000.00";
        assertThat(actualException.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    void createDebitTransaction_isRejected_ifDebitAmountOutOfAcceptedRange() {
        // Given
        Balance savedBalance = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        BigDecimal debitAmount = new BigDecimal("5000.01");
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, debitAmount,
                Operation.WITHDRAW);

        // When-Then
        UnacceptedTransactionAmountException actualException = assertThrows(UnacceptedTransactionAmountException.class,
                () -> underTest.createTransaction(transactionRequest));
        assertThat(actualException.getMessage()).isEqualTo("Maximum accepted Debit Amount is £5,000.00");
    }

    @Test
    void createDebitTransaction_isRejected_ifNotEnoughCreditInBalance() {
        // Given
        BigDecimal existingBalance = new BigDecimal("100.00");
        Balance savedBalance = createBalance(CUSTOMER_ID_1, existingBalance, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        BigDecimal debitAmount = existingBalance.add(new BigDecimal("0.01"));
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, debitAmount,
                Operation.WITHDRAW);

        // When-Then
        UnacceptedTransactionAmountException actualException = assertThrows(UnacceptedTransactionAmountException.class,
                () -> underTest.createTransaction(transactionRequest));
        assertThat(actualException.getMessage()).isEqualTo("Not enough Credit in Balance");
    }

    @Test
    void createCreditTransaction_updatesBalanceAndCreatesCreditTransaction_ifNoExceptions() {
        // Given
        Balance savedBalance = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD);

        // And
        Transaction savedTransaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, TRANSACTION_AMOUNT,
                Operation.ADD, LocalDateTime.now());
        when(transactionRepository.insert(transactionCaptor.capture())).thenReturn(savedTransaction);

        // When
        TransactionBalanceResponse actual = underTest.createTransaction(transactionRequest);

        // Then
        verify(balanceRepository).save(balanceCaptor.capture());
        BigDecimal expectedFinalBalance = BALANCE_CUSTOMER_1.add(TRANSACTION_AMOUNT);
        assertThat(balanceCaptor.getValue())
                .extracting(
                        "customerId",
                        "balanceAmount")
                .containsExactly(
                        CUSTOMER_ID_1,
                        expectedFinalBalance
                );
        assertThat(transactionCaptor.getValue())
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
        assertThat(actual).isEqualTo(TransactionMapper.transactionAndBalanceToTransactionBalanceResponse(CUSTOMER_ID_1,
                savedTransaction, expectedFinalBalance));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "100.00,50.00",
            "100.00,100.00" // all the balance will be withdrawn
    })
    void createDebitTransaction_updatesBalanceAndCreatesDebitTransaction_ifEnoughBalanceAndNoExceptions(
            String existingBalanceStr, String transactionAmountStr) {
        // Given
        BigDecimal existingBalance = new BigDecimal(existingBalanceStr);
        Balance savedBalance = createBalance(CUSTOMER_ID_1, existingBalance, LocalDateTime.now(), LocalDateTime.now());
        when(balanceRepository.findById(CUSTOMER_ID_1)).thenReturn(Optional.of(savedBalance));

        // And
        BigDecimal withdrawAmount = new BigDecimal(transactionAmountStr);
        TransactionRequest transactionRequest = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, withdrawAmount,
                Operation.WITHDRAW);

        // And
        Transaction savedTransaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, withdrawAmount,
                Operation.WITHDRAW, LocalDateTime.now());
        when(transactionRepository.insert(transactionCaptor.capture())).thenReturn(savedTransaction);

        // When
        TransactionBalanceResponse actual = underTest.createTransaction(transactionRequest);

        // Then
        verify(balanceRepository).save(balanceCaptor.capture());
        BigDecimal expectedFinalBalance = existingBalance.subtract(withdrawAmount);
        assertThat(balanceCaptor.getValue())
                .extracting(
                        "customerId",
                        "balanceAmount")
                .containsExactly(
                        CUSTOMER_ID_1,
                        expectedFinalBalance
                );
        assertThat(transactionCaptor.getValue())
                .extracting(
                        "correlationId",
                        "customerId",
                        "amount",
                        "operation")
                .containsExactly(
                        CORRELATION_ID_A,
                        CUSTOMER_ID_1,
                        withdrawAmount,
                        Operation.WITHDRAW
                );
        assertThat(actual).isEqualTo(TransactionMapper.transactionAndBalanceToTransactionBalanceResponse(CUSTOMER_ID_1,
                savedTransaction, expectedFinalBalance));
    }
}