package com.jfrengineering.digitalwallet.web.model;

import com.jfrengineering.digitalwallet.domain.Operation;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {

    @NotNull
    private UUID correlationId;

    @NotNull
    private UUID customerId;

    @NotNull
    @Positive
    @Digits(integer = 5, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private Operation operation;
}
