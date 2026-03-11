package com.capgemini.mprs.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reconciliation_job")
@Getter
@Setter
public class ReconciliationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;   // windowStart
    private LocalDate endDate;     // windowEnd

    @Enumerated(EnumType.STRING)
    private JobStatus status;      // RUNNING, SUCCEEDED, FAILED

    private Instant createdAt;
    private Instant updatedAt;

    public enum JobStatus {
        RUNNING, SUCCEEDED, FAILED
    }


}
