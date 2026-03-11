package com.capgemini.mprs.repositories;

import com.capgemini.mprs.entities.ReconciliationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {


    Optional<ReconciliationJob> findFirstByStartDateAndEndDateOrderByCreatedAtDesc(LocalDate start, LocalDate end);
    boolean existsByStartDateAndEndDateAndStatus(LocalDate start, LocalDate end, ReconciliationJob.JobStatus status);
    boolean existsByStartDateAndEndDate(LocalDate start, LocalDate end);
    ReconciliationJob findJobStatusById(Long id);
}
