package com.capgemini.mprs.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
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


    public ReconciliationException(ReconciliationJob job,
                                   String transactionId,
                                   BigDecimal expectedAmount,
                                   BigDecimal paidAmount,
                                   BigDecimal variance,
                                   ExceptionType exceptionType,
                                   String description) {
        this.job = job;
        this.transactionId = transactionId;
        this.expectedAmount = expectedAmount;
        this.paidAmount = paidAmount;
        this.variance = variance;
        this.exceptionType = exceptionType;
        this.description = description;
    }

    public enum ExceptionType {
        MISSING_PAYOUT,
        PARTIAL_PAYOUT,
        OVERPAYMENT,
        DUPLICATE_PAYOUT,
        INELIGIBLE_PAYOUT
    }

}