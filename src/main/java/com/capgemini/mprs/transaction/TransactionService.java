package com.capgemini.mprs.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> findAllTransactions(){
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findTransactionById(String id){
        return transactionRepository.findById(id);
    }

}
