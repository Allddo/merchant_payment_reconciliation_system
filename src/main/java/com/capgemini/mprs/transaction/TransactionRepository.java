package com.capgemini.mprs.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    boolean existsByTransactionId(String transaction_id);

}
