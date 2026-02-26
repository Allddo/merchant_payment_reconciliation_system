package com.capgemini.mprs.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "payouts")
public class Payout {
    @Id
    @Column(name = "transaction_id", unique = true)
    private Long transaction_id;
    private BigDecimal payout_amount;
    private LocalDate payout_date;

    public Payout(){}

    public Payout(Long transaction_id, BigDecimal payout_amount, LocalDate payout_date){
        this.transaction_id = transaction_id;
        this.payout_amount = payout_amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.payout_date = payout_date;
    }

}
