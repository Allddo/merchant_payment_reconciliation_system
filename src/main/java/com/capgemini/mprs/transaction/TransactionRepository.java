package com.capgemini.mprs.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>, TransactionBatchOps {

    boolean existsByTransactionId(String transaction_id);

    List<Transaction> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);

}
