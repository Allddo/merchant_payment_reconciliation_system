package com.capgemini.mprs.Provider;

import au.com.dius.pact.core.support.Either;
import au.com.dius.pact.provider.junitsupport.State;
import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class TransactionProviderStates {

    @Autowired
    private TransactionRepository transactionRepository;

    @State("transaction with id tx123 exists")
    public void transactionWithIdExists(){
        transactionRepository.deleteAll();

        Transaction transaction = new Transaction();
        transaction.setTransactionId("tx123");
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setStatus(Transaction.Status.SETTLED);
        transaction.setSettlementDate(LocalDate.of(2026, 3, 17));
        transaction.setMerchantId("merchant-001");

        transactionRepository.save(transaction);
    }

}
