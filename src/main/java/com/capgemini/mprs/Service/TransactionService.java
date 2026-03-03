package com.capgemini.mprs.Service;

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

    public Transaction ingestTransaction(String s)
    {
        String[] parts = s.split(",");
        Long id = Long.parseLong(parts[0]);
        BigDecimal amount = new BigDecimal(parts[1]);
        String status = parts[2];

       // DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(parts[3]);

        Integer merchantId = Integer.parseInt(parts[4]);
        return new Transaction(id, amount, status, date, merchantId);
    }

    public Long getCount() {
        return repository.count();
    }

    public List<ExceptionEntity> getExceptions() {
        return exceptionEntityService.findTransactionExceptions();
    }
}