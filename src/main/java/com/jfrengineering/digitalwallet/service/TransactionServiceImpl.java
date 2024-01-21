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
import com.jfrengineering.digitalwallet.web.model.TransactionResponse;
import com.jfrengineering.digitalwallet.web.model.TransactionsPageResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.jfrengineering.digitalwallet.domain.Operation.ADD;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    static final String TRANSACTION_SORTING_FIELD = "createdAt";
    private static final String NOT_FOUND_ERROR_TEMPLATE = "Non existing customer with ID '%s'";

    private final CustomerCacheService customerCacheService;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public TransactionsPageResponse getTransactionsByCustomerId(UUID customerId, int page, int size) {
        if (!customerCacheService.customerBalanceExists(customerId)) {
            throw new EntityNotFoundException(String.format(NOT_FOUND_ERROR_TEMPLATE, customerId));
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(TRANSACTION_SORTING_FIELD).descending());
        Page<Transaction> transactionsPage = transactionRepository.findByCustomerId(customerId, pageRequest);
        List<TransactionResponse> transactionResponseList = transactionsPage.getContent().stream()
                .map(TransactionMapper::transactionToTransactionResponse)
                .toList();
        return new TransactionsPageResponse(customerId, transactionResponseList, pageRequest,
                transactionsPage.getTotalElements());
    }

    @Override
    @Transactional
    public TransactionBalanceResponse createTransaction(TransactionRequest transactionRequest) {
        if (!customerCacheService.customerBalanceExists(transactionRequest.getCustomerId())) {
            throw new EntityNotFoundException(String.format(NOT_FOUND_ERROR_TEMPLATE, transactionRequest.getCustomerId()));
        }
        Balance balance = balanceRepository.findById(transactionRequest.getCustomerId()).orElseThrow();
        BigDecimal updatedBalanceAmount = validateAndCalculateNewBalance(transactionRequest.getOperation(), balance,
                transactionRequest.getAmount());
        balance.setBalanceAmount(updatedBalanceAmount);
        balanceRepository.save(balance); // 'updatedAt' will be updated at the end of the transaction

        Transaction transaction = TransactionMapper.transactionRequestToTransaction(transactionRequest);

        Transaction savedTransaction;
        synchronized (this) {
            savedTransaction = transactionRepository.insert(transaction);
        }

        return TransactionMapper.transactionAndBalanceToTransactionBalanceResponse(transactionRequest.getCustomerId(),
                savedTransaction, updatedBalanceAmount);
    }

    private BigDecimal validateAndCalculateNewBalance(Operation operation, Balance balance, BigDecimal transactionAmount) {
        BigDecimal balanceAmount = balance.getBalanceAmount();
        if (ADD == operation) {
            if (transactionAmount.compareTo(BigDecimal.TEN) < 0) {
                throw new UnacceptedTransactionAmountException("Minimum accepted Credit Amount is £10.00");
            } else if (transactionAmount.compareTo(BigDecimal.valueOf(10_000)) > 0) {
                throw new UnacceptedTransactionAmountException("Maximum accepted Credit Amount is £10,000.00");
            }
            balanceAmount = balanceAmount.add(transactionAmount);
        } else {
            if (transactionAmount.compareTo(BigDecimal.valueOf(5_000)) > 0) {
                throw new UnacceptedTransactionAmountException("Maximum accepted Debit Amount is £5,000.00");
            }
            balanceAmount = balanceAmount.subtract(transactionAmount);
            if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new UnacceptedTransactionAmountException("Not enough Credit in Balance");
            }
        }
        return balanceAmount;
    }
}
