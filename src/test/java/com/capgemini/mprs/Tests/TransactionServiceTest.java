package com.capgemini.mprs.Tests;

import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.repositories.TransactionRepository;
import com.capgemini.mprs.services.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Minimal, fast unit tests for TransactionService.
 * No Spring context is used — Mockito handles all mocking/injection.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("ingestChunk calls saveAll with the provided list")
    void ingestChunk_callsSaveAll() {
        Transaction t1 = new Transaction();
        t1.setTransactionId("T1");
        t1.setAmount(new BigDecimal("10.00"));
        t1.setSettlementDate(LocalDate.of(2024, 2, 1));

        Transaction t2 = new Transaction();
        t2.setTransactionId("T2");
        t2.setAmount(new BigDecimal("20.00"));
        t2.setSettlementDate(LocalDate.of(2024, 2, 2));

        List<Transaction> chunk = List.of(t1, t2);

        transactionService.ingestChunk(chunk);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository, times(1)).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();

        assertThat(saved).containsExactly(t1, t2);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    @DisplayName("findAllTransactions returns repository.findAll result")
    void findAllTransactions_returnsRepoList() {
        Transaction a = new Transaction();
        Transaction b = new Transaction();
        when(transactionRepository.findAll()).thenReturn(List.of(a, b));

        List<Transaction> result = transactionService.findAllTransactions();

        assertThat(result).containsExactly(a, b);
        verify(transactionRepository, times(1)).findAll();
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    @DisplayName("findTransactionById returns Optional with entity when present")
    void findTransactionById_present() {
        Transaction t = new Transaction();
        t.setTransactionId("X");

        when(transactionRepository.findById("X")).thenReturn(Optional.of(t));

        Optional<Transaction> result = transactionService.findTransactionById("X");

        assertThat(result).isPresent().contains(t);
        verify(transactionRepository, times(1)).findById("X");
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    @DisplayName("findTransactionById returns Optional.empty when not found")
    void findTransactionById_empty() {
        when(transactionRepository.findById("404")).thenReturn(Optional.empty());

        Optional<Transaction> result = transactionService.findTransactionById("404");

        assertThat(result).isEmpty();
        verify(transactionRepository, times(1)).findById("404");
        verifyNoMoreInteractions(transactionRepository);
    }
}