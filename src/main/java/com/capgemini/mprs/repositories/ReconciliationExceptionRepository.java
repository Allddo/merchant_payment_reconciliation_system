package com.capgemini.mprs.repositories;

import com.capgemini.mprs.entities.ReconciliationException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationException,Long> {
}
