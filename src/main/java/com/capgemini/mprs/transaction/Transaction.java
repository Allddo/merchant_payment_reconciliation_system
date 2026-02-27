package com.capgemini.mprs.transaction;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_transactions_txn_id", columnNames = "transactionId"))
public class Transaction {

    @Id
    @Column(name="transaction_id", length = 64, nullable = false)
    private String transactionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    public enum Status{ AUTHORIZED, SETTLED, REFUNDED, CHARGEBACK }

}
