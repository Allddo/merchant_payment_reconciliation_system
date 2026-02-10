package com.capgemini.mprs.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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

    public Transaction get(Integer id)
    {
        return repository.findById(id).orElse(null);
    }

}