package com.capgemini.mprs.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Table(name = "reconciliation_summary")
public class ReconciliationSummary {

    @Id
    private Long jobId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "job_id")
    private ReconciliationJob job;

    private Long totalEligibleTransactions;
    private BigDecimal totalExpectedPayout;
    private BigDecimal totalPaid;
    private BigDecimal totalVariance;


    private Long missingPayoutCount;
    private Long partialPayoutCount;
    private Long overpaymentCount;
    private Long ineligiblePayoutCount;

}