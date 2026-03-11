package com.capgemini.mprs.repositories;

import com.capgemini.mprs.entities.ReconciliationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationSummaryRepository extends JpaRepository<ReconciliationSummary,Long> {
}
