package com.capgemini.mprs.Tests;

import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.repositories.ReconciliationExceptionRepository;
import com.capgemini.mprs.services.ReconciliationExceptionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReconciliationExceptionServiceTest {

    private final ReconciliationExceptionRepository repository = mock(ReconciliationExceptionRepository.class);
    private final ReconciliationExceptionService service = new ReconciliationExceptionService(repository);

    @Test
    void createException_shouldBuildAndSaveReconciliationException() {

        ReconciliationJob job = new ReconciliationJob();

        String transactionId = "TX123";
        BigDecimal expectedAmount = BigDecimal.TEN;
        BigDecimal paidAmount = BigDecimal.ONE;
        BigDecimal variance = expectedAmount.subtract(paidAmount);
        ReconciliationException.ExceptionType type = ReconciliationException.ExceptionType.PARTIAL_PAYOUT;
        String description = "Amount mismatch detected";

        ReconciliationException savedEntity =
                new ReconciliationException(job, transactionId, expectedAmount, paidAmount, variance, type, description);

        when(repository.save(any(ReconciliationException.class))).thenReturn(savedEntity);

        ReconciliationException result = service.createException(
                job, transactionId, expectedAmount, paidAmount, variance, type, description
        );

        ArgumentCaptor<ReconciliationException> captor =
                ArgumentCaptor.forClass(ReconciliationException.class);

        verify(repository).save(captor.capture());
        ReconciliationException captured = captor.getValue();

        assertThat(captured.getJob()).isEqualTo(job);
        assertThat(captured.getTransactionId()).isEqualTo(transactionId);
        assertThat(captured.getExpectedAmount()).isEqualTo(expectedAmount);
        assertThat(captured.getPaidAmount()).isEqualTo(paidAmount);
        assertThat(captured.getVariance()).isEqualTo(variance);
        assertThat(captured.getExceptionType()).isEqualTo(type);
        assertThat(captured.getDescription()).isEqualTo(description);

        assertThat(result).isEqualTo(savedEntity);
    }
}