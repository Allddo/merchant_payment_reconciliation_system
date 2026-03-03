package com.capgemini.mprs.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "transaction_id", unique = true)
    private Long transaction_id;
    private BigDecimal amount;
    private String status;
    private LocalDate settlement_date;
    private Integer merchant_id;

    public Transaction() {
    }

    public Transaction(Long transaction_id, BigDecimal amount, String status, LocalDate settlement_date, Integer merchant_id) {
        this.transaction_id = transaction_id;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.status = status;
        this.settlement_date = settlement_date;
        this.merchant_id = merchant_id;
    }

}
