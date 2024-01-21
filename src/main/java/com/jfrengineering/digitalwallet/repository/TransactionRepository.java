package com.jfrengineering.digitalwallet.repository;

import com.jfrengineering.digitalwallet.domain.Transaction;
import jakarta.persistence.EntityExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface TransactionRepository extends PagingAndSortingRepository<Transaction, UUID>, CrudRepository<Transaction, UUID> {
    Page<Transaction> findByCustomerId(UUID customerId, Pageable pageable);

    default Transaction insert(Transaction transaction) {
        if (existsById(transaction.getCorrelationId())) {
            throw new EntityExistsException("Transaction with the same correlationId already exists");
        }
        return save(transaction);
    }
}
