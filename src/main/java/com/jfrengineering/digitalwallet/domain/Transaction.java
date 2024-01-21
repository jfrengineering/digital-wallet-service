package com.jfrengineering.digitalwallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = @Index(name = "customer_index", columnList = "customerId"))
public class Transaction {

    @Id
    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    private UUID correlationId;

    @Column(length = 36, columnDefinition = "varchar", updatable = false, nullable = false)
    private UUID customerId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Operation operation;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;
}
