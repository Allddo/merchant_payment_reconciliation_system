package com.capgemini.mprs.payout;

import com.capgemini.mprs.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayoutService {
    private final PayoutRepository repository;
    @Autowired
    public PayoutService(PayoutRepository repository)
    {
        this.repository = repository;
    }

    public List<Payout> createPayouts(List<Payout> payouts)
    {
        for (Payout payout: payouts){
            repository.save(payout);
        }
        return payouts;
    }

    public Payout get(Integer id)
    {
        return repository.findById(id).orElse(null);
    }

}
