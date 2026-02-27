package com.capgemini.mprs.exception;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationException,Long> {
}
