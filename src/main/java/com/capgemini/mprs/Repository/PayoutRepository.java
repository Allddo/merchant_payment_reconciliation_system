package com.capgemini.mprs.Repository;

import com.capgemini.mprs.Entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout,Long> {
}
