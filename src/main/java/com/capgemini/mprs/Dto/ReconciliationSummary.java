package com.capgemini.mprs.Dto;


import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummary {
    @Id
    @Column(name = "transaction_id", unique = true)
    private Long transaction_id;
    private BigDecimal payout;
    private BigDecimal expectedPayout;
    private boolean exception;
}
