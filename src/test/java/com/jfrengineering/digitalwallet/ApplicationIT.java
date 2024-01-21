package com.jfrengineering.digitalwallet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrengineering.digitalwallet.domain.Balance;
import com.jfrengineering.digitalwallet.domain.Operation;
import com.jfrengineering.digitalwallet.domain.Transaction;
import com.jfrengineering.digitalwallet.mapper.TransactionMapper;
import com.jfrengineering.digitalwallet.repository.BalanceRepository;
import com.jfrengineering.digitalwallet.repository.TransactionRepository;
import com.jfrengineering.digitalwallet.service.CustomerCacheServiceImpl;
import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.BALANCE_CUSTOMER_2;
import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_A;
import static com.jfrengineering.digitalwallet.util.TestUtils.CORRELATION_ID_B;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_1;
import static com.jfrengineering.digitalwallet.util.TestUtils.CUSTOMER_ID_2;
import static com.jfrengineering.digitalwallet.util.TestUtils.createBalance;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransaction;
import static com.jfrengineering.digitalwallet.util.TestUtils.createTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationIT {

    private static final String ENDPOINT = "/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setUp() {
        Balance balanceCustomer1 = createBalance(CUSTOMER_ID_1, BALANCE_CUSTOMER_1);
        Balance balanceCustomer2 = createBalance(CUSTOMER_ID_2, BALANCE_CUSTOMER_2);
        balanceRepository.saveAll(List.of(balanceCustomer1, balanceCustomer2));

        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(CustomerCacheServiceImpl.class)).addAppender(logWatcher);
    }

    @AfterEach
    void tearDown() {
        balanceRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void createTransaction_failsValidation_ifNullCorrelationId() throws Exception {
        // Given
        UUID correlationId = null;
        TransactionRequest transaction = createTransactionRequest(correlationId, CUSTOMER_ID_1, BigDecimal.TEN, Operation.ADD);

        // When-Then
        performRequestAndVerifyValidationFailure(transaction, "[\"'correlationId' must not be null\"]");
    }

    @Test
    void createTransaction_failsValidation_ifNullCustomerId() throws Exception {
        // Given
        UUID customerId = null;
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, customerId, BigDecimal.TEN, Operation.ADD);

        // When-Then
        performRequestAndVerifyValidationFailure(transaction, "[\"'customerId' must not be null\"]");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "-123.45", "0.00" })
    void createTransaction_failsValidation_ifNullOrNonPositiveAmount(String amountStr) throws Exception {
        // Given
        BigDecimal transactionAmount = amountStr == null ? null : new BigDecimal(amountStr);
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, transactionAmount, Operation.ADD);
        String expectedResponseMessage = amountStr == null ? "[\"'amount' must not be null\"]" : "[\"'amount' must be greater than 0\"]";

        // When-Then
        performRequestAndVerifyValidationFailure(transaction, expectedResponseMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = { "123.456", "10.000", "123456" })
    void createTransaction_failsValidation_ifAmountWithMoreThanTwoDigitsOrMoreThanFiveIntegers(String amountStr) throws Exception {
        // Given
        BigDecimal transactionAmount = new BigDecimal(amountStr);
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, transactionAmount, Operation.ADD);
        String expectedResponseMessage = "[\"'amount' numeric value out of bounds (<5 digits>.<2 digits> expected)\"]";

        // When-Then
        performRequestAndVerifyValidationFailure(transaction, expectedResponseMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = { "9.99", "10000.01" })
    void createCreditTransaction_failsValidation_ifCreditedAmountOutOfAcceptedRange(String amountStr) throws Exception {
        // Given
        BigDecimal transactionAmount = new BigDecimal(amountStr);
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, transactionAmount,
                Operation.ADD);
        String expectedReason = transactionAmount.compareTo(BigDecimal.TEN) < 0
                ? "Minimum accepted Credit Amount is £10.00"
                : "Maximum accepted Credit Amount is £10,000.00";
        String expectedResponseMessage = "Transaction rejected. " + expectedReason;

        // When-Then
        performRequestAndVerifyUnacceptedTransactionAmount(transaction, expectedResponseMessage);
    }

    @Test
    void createDebitTransaction_failsValidation_ifDebitAmountOutOfAcceptedRange() throws Exception {
        // Given
        BigDecimal transactionAmount = new BigDecimal("5000.01");
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_1, transactionAmount,
                Operation.WITHDRAW);
        String expectedResponseMessage = "Transaction rejected. Maximum accepted Debit Amount is £5,000.00";

        // When-Then
        performRequestAndVerifyUnacceptedTransactionAmount(transaction, expectedResponseMessage);
    }

    @Test
    void createDebitTransaction_failsValidation_ifNotEnoughCreditInBalance() throws Exception {
        // Given
        BigDecimal existingBalanceAmount = balanceRepository.findById(CUSTOMER_ID_2).get().getBalanceAmount();
        BigDecimal amountToWithdraw = existingBalanceAmount.add(new BigDecimal("0.01"));
        TransactionRequest transaction = createTransactionRequest(CORRELATION_ID_A, CUSTOMER_ID_2, amountToWithdraw,
                Operation.WITHDRAW);
        String expectedResponseMessage = "Transaction rejected. Not enough Credit in Balance";

        // When-Then
        performRequestAndVerifyUnacceptedTransactionAmount(transaction, expectedResponseMessage);
    }

    @Test
    void createTransaction_isRejected_ifThereIsPreviousTransactionWithSameCorrelationId() throws Exception {
        // Given a correlationId
        UUID correlationId = UUID.randomUUID();

        // And a transaction with that correlationId was processed previously
        Transaction existingTransaction = createTransaction(correlationId, CUSTOMER_ID_1, BigDecimal.TEN, Operation.ADD);
        transactionRepository.save(existingTransaction);

        // And a new transaction with the same correlationId
        TransactionRequest newTransaction = createTransactionRequest(correlationId, CUSTOMER_ID_2, BigDecimal.ONE, Operation.WITHDRAW);

        // When-Then
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransaction)))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo("Transaction rejected. Another transaction with the same 'correlationId' was previously processed");
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class)
    void createTransaction_updatesCustomerBalance(Operation operation) throws Exception {
        // Given a transaction with a correlationId that was processed previously
        Transaction existingTransaction = createTransaction(CORRELATION_ID_A, CUSTOMER_ID_1, BigDecimal.TEN, Operation.ADD);
        transactionRepository.save(existingTransaction);

        // And the customer has a certain balance
        BigDecimal existingBalanceAmount = balanceRepository.findById(CUSTOMER_ID_1).get().getBalanceAmount();

        // And a new transaction with a different correlationId
        TransactionRequest newTransaction = createTransactionRequest(CORRELATION_ID_B, CUSTOMER_ID_1, BigDecimal.TEN, operation);

        // When
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransaction)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        TransactionBalanceResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                TransactionBalanceResponse.class);
        assertThat(response.getCustomerId()).isEqualTo(newTransaction.getCustomerId());
        assertThat(response.getTransaction())
                .extracting(
                        "correlationId",
                        "amount",
                        "operation")
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(
                        newTransaction.getCorrelationId(),
                        newTransaction.getAmount(),
                        operation
                );
        assertThat(response.getUpdatedBalance())
                .isEqualTo(operation == Operation.ADD
                        ? existingBalanceAmount.add(newTransaction.getAmount())
                        : existingBalanceAmount.subtract(newTransaction.getAmount()));
    }

    @Test
    void getCustomerTransactions_returnsNotFound_ifNonExistingCustomer() throws Exception {
        // Given
        UUID nonExistentCustomerId = UUID.randomUUID();

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT + "/" + nonExistentCustomerId))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo(String.format("Non existing customer with ID '%s'", nonExistentCustomerId));
    }

    @Test
    void getCustomerTransactions_returnsEmptyList_ifCustomerHasNoTransactions() throws Exception {
        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT + "/" + CUSTOMER_ID_1))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        TransactionsPageResponse transactionsPageResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                TransactionsPageResponse.class);
        assertThat(transactionsPageResponse).isEmpty();
    }

    @Test
    void getCustomerTransactions_returnsExistingTransactionsPaginated() throws Exception {
        // Given non-cached customerId with existing balance
        UUID customerId = UUID.randomUUID();
        balanceRepository.save(createBalance(customerId, new BigDecimal("0.00")));

        // Given existing transactions
        List<Transaction> transactions = List.of(
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("10000"), Operation.ADD),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("5000"), Operation.WITHDRAW),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("10"), Operation.ADD),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("5000"), Operation.WITHDRAW),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("10"), Operation.ADD),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("20"), Operation.WITHDRAW),
                createTransaction(UUID.randomUUID(), customerId, new BigDecimal("123.45"), Operation.ADD)
        );
        List<Transaction> savedTransactions = new ArrayList<>(); // saved-transactions include `createdAt` timestamp
        transactions.forEach(transaction -> {
            savedTransactions.add(transactionRepository.save(transaction));
            // Await some time before next transaction
            await().pollDelay(1, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(true));
        });

        // When requesting first page
        MvcResult mvcResult = performPageRequest(customerId, 0 , 3);

        // Then the two most recent transactions are returned
        verifyPageResult(mvcResult, List.of(savedTransactions.get(6), savedTransactions.get(5), savedTransactions.get(4)));

        // When requesting second page
        mvcResult = performPageRequest(customerId, 1 , 3);

        // Then the two most recent transactions are returned
        verifyPageResult(mvcResult, List.of(savedTransactions.get(3), savedTransactions.get(2), savedTransactions.get(1)));

        // When requesting third page
        mvcResult = performPageRequest(customerId, 2 , 3);

        // Then the two most recent transactions are returned
        verifyPageResult(mvcResult, List.of(savedTransactions.get(0)));

        // And cache was used to verify if given customerId exists
        AssertionsForClassTypes.assertThat(logWatcher.list.size()).isEqualTo(1); // only one call to database
        assertThat(logWatcher.list.get(0))
                .extracting("level", "formattedMessage")
                .containsExactly(Level.INFO, "Hitting the database to verify if customer's Balance exists, as not cached yet");
    }

    private void performRequestAndVerifyValidationFailure(TransactionRequest transaction, String expectedResponseMessage)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo(expectedResponseMessage);
    }

    private void performRequestAndVerifyUnacceptedTransactionAmount(TransactionRequest transaction, String expectedResponseMessage)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isNotAcceptable())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo(expectedResponseMessage);
    }

    private MvcResult performPageRequest(UUID customerId, int pageNumber, int pageSize) throws Exception {
        return mockMvc.perform(get(ENDPOINT + "/" + customerId)
                        .param("pageNumber", String.valueOf(pageNumber))
                        .param("pageSize",  String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void verifyPageResult(MvcResult mvcResult, List<Transaction> expectedTransactions) throws Exception {
        List<TransactionResponse> expectedTransactionResponseList = expectedTransactions.stream()
                .map(TransactionMapper::transactionToTransactionResponse)
                .toList();
        TransactionsPageResponse transactionsPageResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                TransactionsPageResponse.class);
        assertThat(transactionsPageResponse)
                .hasSize(expectedTransactions.size())
                .containsAll(expectedTransactionResponseList);
    }
}
