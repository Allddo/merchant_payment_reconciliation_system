package com.capgemini.mprs.repositories;

import com.capgemini.mprs.entities.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, String>{

    List<Payout> findByPayoutDateBetween(LocalDate startDate, LocalDate endDate);

}