package com.jfrengineering.digitalwallet.web.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionBalanceResponse {
    private final UUID customerId;
    private final TransactionResponse transaction;
    private final BigDecimal updatedBalance;
}
