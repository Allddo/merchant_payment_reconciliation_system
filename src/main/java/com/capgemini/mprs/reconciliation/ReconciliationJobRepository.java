package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {


    Optional<ReconciliationJob> findFirstByStartDateAndEndDateOrderByCreatedAtDesc(LocalDate start, LocalDate end);
    boolean existsByStartDateAndEndDateAndStatus(LocalDate start, LocalDate end, ReconciliationJob.JobStatus status);
    ReconciliationJob.JobStatus findJobStatusById(Long id);
}
