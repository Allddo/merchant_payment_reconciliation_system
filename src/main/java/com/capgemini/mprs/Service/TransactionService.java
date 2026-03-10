package com.capgemini.mprs.Service;

import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Entity.Transaction;
import com.capgemini.mprs.Repository.PayoutRepository;
import com.capgemini.mprs.Repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository repository;
    private final ExceptionEntityService exceptionEntityService;

    @Autowired
    public TransactionService(TransactionRepository repository, ExceptionEntityService exceptionEntityService) {
        this.repository = repository;
        this.exceptionEntityService = exceptionEntityService;
     }


    public List<Transaction> createTransactions(List<Transaction> transactions)
    {
        for(Transaction transaction: transactions)
        {
            repository.save(transaction);
        }
        return transactions;
    }

    public List<Transaction> getAll(Integer amount)//Switch to page on website ui
    {

        PageRequest pageRequest = PageRequest.of(0, amount);
        return repository.findAll(pageRequest).getContent();
    }

    public ProcessingResult ingestTransaction(String s)
    {
        ProcessingResult r = new ProcessingResult();
        try{
            String[] parts = s.split(",");
            Long id = Long.parseLong(parts[0]);
            BigDecimal amount = new BigDecimal(parts[1]);
            if(amount.compareTo(BigDecimal.valueOf(0.00)) < 0)
            {
                throw new Exception("Amount cannot be negative");
            }
            String status = parts[2];
            if(!(status.equals("AUTHORIZED") || status.equals("CHARGEBACK") || status.equals("REFUNDED") || status.equals("SETTLED")))
            {
                throw new Exception("Invalid status");
            }
            // DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate date = LocalDate.parse(parts[3]);

            Integer merchantId = Integer.parseInt(parts[4]);
            r.setTransaction(new Transaction(id, amount, status, date, merchantId));
        }
        catch (Exception e){
            r.setError(new ExceptionEntity(400, "Bad Request for transaction: " + s, e.getMessage(), "/api/v1/transactions/bulk"));
        }
        return r;
    }

    public Long getCount() {
        return repository.count();
    }

    public List<ExceptionEntity> getExceptions() {
        return exceptionEntityService.findTransactionExceptions();
    }
}