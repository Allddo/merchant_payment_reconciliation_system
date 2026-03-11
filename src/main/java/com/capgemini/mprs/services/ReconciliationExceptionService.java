package com.capgemini.mprs.services;

import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.repositories.ReconciliationExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReconciliationExceptionService {

    private final ReconciliationExceptionRepository repository;

    public ReconciliationException createException(
            ReconciliationJob job,
            String transactionId,
            BigDecimal expectedAmount,
            BigDecimal paidAmount,
            BigDecimal variance,
            ReconciliationException.ExceptionType type,
            String description
    ) {
        ReconciliationException e = new ReconciliationException(job, transactionId, expectedAmount,paidAmount,variance,type,description);
        return repository.save(e);
    }
}