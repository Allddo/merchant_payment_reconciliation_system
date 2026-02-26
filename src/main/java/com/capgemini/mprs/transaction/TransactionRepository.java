package com.capgemini.mprs.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {


}
