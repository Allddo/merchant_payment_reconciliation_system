package com.capgemini.mprs.Repository;

import com.capgemini.mprs.Entity.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<Reconciliation,Long> {
}
