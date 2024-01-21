package com.jfrengineering.digitalwallet.web.model;

import com.jfrengineering.digitalwallet.domain.Operation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public class TransactionResponse {
    private final UUID correlationId;
    private final BigDecimal amount;
    private final Operation operation;
    private final String createdAt;
}
