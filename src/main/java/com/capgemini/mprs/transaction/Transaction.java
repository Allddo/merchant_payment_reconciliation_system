package com.capgemini.mprs.transaction;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "transaction_id", unique = true)
    private Integer transaction_id;
    private Double amount;
    private String status;
    private String settlement_date;
    private Integer merchant_id;

    public Transaction(){}
    public Transaction(Integer transaction_id, Double amount, String status, String settlement_date, Integer merchant_id)
    {
        this.transaction_id = transaction_id;
        this.amount = amount;
        this.status = status;
        this.settlement_date = settlement_date;
        this.merchant_id = merchant_id;
    }

    public Integer getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(Integer transaction_id) {
        this.transaction_id = transaction_id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSettlement_date() {
        return settlement_date;
    }

    public void setSettlement_date(String settlement_date) {
        this.settlement_date = settlement_date;
    }

    public Integer getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(Integer merchant_id) {
        this.merchant_id = merchant_id;
    }
}
