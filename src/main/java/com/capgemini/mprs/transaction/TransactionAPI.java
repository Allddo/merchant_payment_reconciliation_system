package com.capgemini.mprs.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
public class TransactionAPI {
    private final TransactionService service;

    @Autowired
    public TransactionAPI(TransactionService service) {
        this.service = service;
    }

    // Get transaction by an id of some sort
    @GetMapping("/{id}")
    public Transaction getTransaction(@PathVariable Integer id) {
        return service.get(id);
    }


    @PostMapping("/bulk")
    public ResponseEntity<?> addTransaction(@RequestBody List<Transaction> transactions)
    {
        service.createTransactions(transactions);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactions);
    }

}
