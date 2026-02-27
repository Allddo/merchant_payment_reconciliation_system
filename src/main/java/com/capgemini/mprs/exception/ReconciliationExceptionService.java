package com.capgemini.mprs.exception;

import com.capgemini.mprs.reconciliation.ReconciliationJob;
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
        ReconciliationException e = new ReconciliationException();
        e.setJob(job);
        e.setTransactionId(transactionId);
        e.setExpectedAmount(expectedAmount);
        e.setPaidAmount(paidAmount);
        e.setVariance(variance);

        if (expectedAmount != null && paidAmount != null) {
            e.setVariance((paidAmount.subtract(expectedAmount)).abs());
        } else {
            e.setVariance(BigDecimal.valueOf(0));
        }

        e.setExceptionType(type);
        e.setDescription(description);

        return repository.save(e);
    }
}