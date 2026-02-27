package com.capgemini.mprs.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public void ingestChunk(List<Transaction> chunk) {
        transactionRepository.saveInBatches(chunk);
    }

    public List<Transaction> findAllTransactions(){
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findTransactionById(String id){
        return transactionRepository.findById(id);
    }

}
