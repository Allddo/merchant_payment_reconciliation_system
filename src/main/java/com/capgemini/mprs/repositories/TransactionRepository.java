package com.capgemini.mprs.repositories;

import com.capgemini.mprs.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>{

    boolean existsByTransactionId(String transaction_id);

    List<Transaction> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);

}
