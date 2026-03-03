package com.capgemini.mprs.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "reconciliations")
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id", unique = true,updatable = false, nullable = false)
    private Long job_id;
    private Long totalEligibleTransactions = 0L;
    private BigDecimal totalExpectedPayout = BigDecimal.ZERO;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private BigDecimal totalVariance = BigDecimal.ZERO;
    private Long totalExceptions = 0L;
    private String status = "PENDING";
    //private Summary summary;
//    private List<Exception> exceptions;

    public Reconciliation(){}



}
