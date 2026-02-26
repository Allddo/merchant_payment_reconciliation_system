package com.capgemini.mprs.payout;

import com.capgemini.mprs.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
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

    public List<Payout> getAll(Integer amount)
    {
        PageRequest pageRequest = PageRequest.of(0, amount);
        return repository.findAll(pageRequest).getContent();
    }

    public Payout ingestPayout(String s)
    {
        String[] parts = s.split(",");
        Long id = Long.parseLong(parts[0]);
        BigDecimal payout_amount = new BigDecimal(parts[1]);
       // DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate merchant_id = LocalDate.parse(parts[2]);
        return new Payout(id, payout_amount, merchant_id);
    }

}
