package com.jfrengineering.digitalwallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "balances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {

    @Id
    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    private UUID customerId;

    private BigDecimal balanceAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;
}
