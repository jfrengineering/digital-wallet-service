package com.jfrengineering.digitalwallet.repository;

import com.jfrengineering.digitalwallet.domain.Balance;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface BalanceRepository extends CrudRepository<Balance, UUID> {
}
