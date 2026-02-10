package com.capgemini.mprs.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payouts")
public class Payout {
    @Id
    @Column(name = "transaction_id", unique = true)
    private Integer transaction_id;
    private Double payout_amount;
    private String payout_date;

    public Payout(){}

    public Payout(Integer transaction_id, Double payout_amount, String payout_date){
        this.transaction_id = transaction_id;
        this.payout_amount = payout_amount;
        this.payout_date = payout_date;
    }
    public Integer getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(Integer transaction_id) {
        this.transaction_id = transaction_id;
    }

    public Double getPayout_amount() {
        return payout_amount;
    }

    public void setPayout_amount(Double payout_amount) {
        this.payout_amount = payout_amount;
    }

    public String getPayout_date() {
        return payout_date;
    }

    public void setPayout_date(String payout_date) {
        this.payout_date = payout_date;
    }
}
