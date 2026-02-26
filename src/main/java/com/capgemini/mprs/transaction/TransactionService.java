package com.capgemini.mprs.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository repository;

    @Autowired
    public TransactionService(TransactionRepository repository)
    {
        this.repository = repository;
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

}