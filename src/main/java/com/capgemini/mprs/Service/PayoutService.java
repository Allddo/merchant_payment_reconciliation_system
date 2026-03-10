package com.capgemini.mprs.Service;

import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Entity.Payout;
import com.capgemini.mprs.Repository.PayoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayoutService {
    private final PayoutRepository repository;
    private final ExceptionEntityService exceptionEntityService;
    @Autowired
    public PayoutService(PayoutRepository repository, ExceptionEntityService exceptionEntityService) {
        this.repository = repository;
        this.exceptionEntityService = exceptionEntityService;
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

    public ProcessingResult ingestPayout(String s)
    {
        ProcessingResult r = new ProcessingResult();
        try {
            String[] parts = s.split(",");
            Long id = Long.parseLong(parts[0]);
            BigDecimal payout_amount = new BigDecimal(parts[1]);
            if(payout_amount.compareTo(BigDecimal.valueOf(0.00)) < 0)
            {
                throw new Exception("Amount cannot be negative");
            }
            // DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate merchant_id = LocalDate.parse(parts[2]);
            r.setPayout(new Payout(id, payout_amount, merchant_id));
        }
        catch(Exception e) {
            r.setError(new ExceptionEntity(400, "Bad Request for payout: " + s, e.getMessage(), "/api/v1/payouts/bulk"));
        }
        return r;
    }

    public Long getCount() {
        return repository.count();
    }

    public List<ExceptionEntity> getExceptions() {
        return exceptionEntityService.findPayoutExceptions();
    }
}
