package com.jfrengineering.digitalwallet.service;

import com.jfrengineering.digitalwallet.web.model.TransactionBalanceResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionRequest;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;

import java.util.UUID;

public interface TransactionService {
    TransactionsPageResponse getTransactionsByCustomerId(UUID customerId, int pageNumber, int pageSize);
    TransactionBalanceResponse createTransaction(TransactionRequest transactionRequest);
}
