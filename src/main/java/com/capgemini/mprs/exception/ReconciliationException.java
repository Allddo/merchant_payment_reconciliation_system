package com.capgemini.mprs.exception;

import com.capgemini.mprs.reconciliation.ReconciliationJob;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@RequiredArgsConstructor
@Setter
@Getter
@Table(name = "reconciliation_exception")
public class ReconciliationException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private ReconciliationJob job;

    private String transactionId;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal variance;

    @Enumerated(EnumType.STRING)
    private ExceptionType exceptionType;

    private String description;

    public enum ExceptionType {
        MISSING_PAYOUT,
        PARTIAL_PAYOUT,
        OVERPAYMENT,
        DUPLICATE_PAYOUT,
        INELIGIBLE_PAYOUT
    }

}