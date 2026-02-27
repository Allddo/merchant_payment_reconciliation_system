package com.capgemini.mprs.payout;

import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String> , PayoutBatchOps{

    List<Payout> findByPayoutDateBetween(LocalDate startDate, LocalDate endDate);

}