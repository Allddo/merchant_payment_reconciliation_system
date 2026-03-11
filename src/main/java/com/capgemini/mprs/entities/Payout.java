package com.capgemini.mprs.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name="payouts")
public class Payout {

    @Id
    @Column(name="transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name="payout_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal payoutAmount;

    @Column(name="payout_date", nullable = false)
    private LocalDate payoutDate;

}
