package com.capgemini.mprs.Repository;

import com.capgemini.mprs.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {


}
