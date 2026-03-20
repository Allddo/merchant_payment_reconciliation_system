package com.capgemini.mprs.Tests;

import com.capgemini.mprs.dtos.JobDto;
import com.capgemini.mprs.dtos.RunRequestDto;
import com.capgemini.mprs.entities.Payout;
import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.entities.ReconciliationSummary;
import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.repositories.PayoutRepository;
import com.capgemini.mprs.repositories.ReconciliationExceptionRepository;
import com.capgemini.mprs.repositories.ReconciliationJobRepository;
import com.capgemini.mprs.repositories.ReconciliationSummaryRepository;
import com.capgemini.mprs.repositories.TransactionRepository;
import com.capgemini.mprs.services.ReconciliationExceptionService;
import com.capgemini.mprs.services.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReconciliationServiceTest {

    @Autowired
    private ReconciliationService reconciliationService;

    // Mock all collaborators used by the service
    @MockitoBean private TransactionRepository transactionRepo;
    @MockitoBean private PayoutRepository payoutRepo;
    @MockitoBean private ReconciliationJobRepository jobRepo;
    @MockitoBean private ReconciliationSummaryRepository summaryRepo;
    @MockitoBean private ReconciliationExceptionRepository exceptionRepo;
    @MockitoBean private ReconciliationExceptionService reconciliationExceptionService; // bean required by service

    @Test
    @DisplayName("run(): saves summary, saves exceptions when job for dates does not exist, and marks job SUCCEEDED")
    void run_savesSummary_andExceptions_whenJobDoesNotExist() {
        // Date window to reconcile
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end   = LocalDate.of(2024, 1, 31);

        // Build 4 SETTLED transactions: exact, partial, missing, overpayment
        Transaction t1 = new Transaction();
        t1.setTransactionId("TX-1");
        t1.setAmount(new BigDecimal("100.00")); // expected = 97.20
        t1.setStatus(Transaction.Status.SETTLED);
        t1.setSettlementDate(LocalDate.of(2024, 1, 10));

        Transaction t2 = new Transaction();
        t2.setTransactionId("TX-2");
        t2.setAmount(new BigDecimal("50.00"));  // expected = 48.45 (partial)
        t2.setStatus(Transaction.Status.SETTLED);
        t2.setSettlementDate(LocalDate.of(2024, 1, 11));

        Transaction t3 = new Transaction();
        t3.setTransactionId("TX-3");
        t3.setAmount(new BigDecimal("10.00"));  // expected = 9.45 (missing payout)
        t3.setStatus(Transaction.Status.SETTLED);
        t3.setSettlementDate(LocalDate.of(2024, 1, 12));

        Transaction t4 = new Transaction();
        t4.setTransactionId("TX-4");
        t4.setAmount(new BigDecimal("5.00"));   // expected = 4.58 (overpayment)
        t4.setStatus(Transaction.Status.SETTLED);
        t4.setSettlementDate(LocalDate.of(2024, 1, 13));

        List<Transaction> txs = List.of(t1, t2, t3, t4);

        // Payouts: TX-1 (exact), TX-2 (partial), TX-4 (over)
        Payout p1 = new Payout();
        p1.setTransactionId("TX-1");
        p1.setPayoutAmount(new BigDecimal("97.20"));

        Payout p2 = new Payout();
        p2.setTransactionId("TX-2");
        p2.setPayoutAmount(new BigDecimal("40.00"));

        Payout p4 = new Payout();
        p4.setTransactionId("TX-4");
        p4.setPayoutAmount(new BigDecimal("10.00"));

        List<Payout> payouts = List.of(p1, p2, p4);

        // Stubs
        when(jobRepo.existsByStartDateAndEndDate(start, end)).thenReturn(false);
        when(jobRepo.save(any(ReconciliationJob.class))).thenAnswer(invocation -> {
            ReconciliationJob j = invocation.getArgument(0);
            if (j.getId() == null) j.setId(1L);
            return j;
        });
        when(transactionRepo.findBySettlementDateBetween(start, end)).thenReturn(txs);
        when(payoutRepo.findByPayoutDateBetween(start, end)).thenReturn(payouts);
        when(payoutRepo.findById("TX-1")).thenReturn(Optional.of(p1));
        when(payoutRepo.findById("TX-2")).thenReturn(Optional.of(p2));
        when(payoutRepo.findById("TX-3")).thenReturn(Optional.empty());
        when(payoutRepo.findById("TX-4")).thenReturn(Optional.of(p4));
        when(summaryRepo.save(any(ReconciliationSummary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        RunRequestDto req = new RunRequestDto();
        req.setStartDate(start);
        req.setEndDate(end);
        JobDto jobDto = reconciliationService.run(req);

        // Verify the window fetch calls explicitly
        verify(transactionRepo, times(1)).findBySettlementDateBetween(start, end);
        verify(payoutRepo, times(1)).findByPayoutDateBetween(start, end);

        // Verify job existence check and job saves (created RUNNING, then updated SUCCEEDED)
        verify(jobRepo, times(1)).existsByStartDateAndEndDate(start, end);
        verify(jobRepo, atLeast(1)).save(any(ReconciliationJob.class));

        // Capture and assert summary
        ArgumentCaptor<ReconciliationSummary> summaryCaptor = ArgumentCaptor.forClass(ReconciliationSummary.class);
        verify(summaryRepo, times(1)).save(summaryCaptor.capture());
        ReconciliationSummary savedSummary = summaryCaptor.getValue();

        // Expected per txn after fees: amount - 2.5% - 0.30, HALF_UP(2)
        // TX-1: 100.00 -> 97.20
        // TX-2: 50.00  -> 48.45
        // TX-3: 10.00  ->  9.45
        // TX-4: 5.00   ->  4.58
        // Totals:
        //   totalExpected = 97.20 + 48.45 + 9.45 + 4.58 = 159.68
        //   totalPaid     = 97.20 + 40.00 + 10.00       = 147.20
        //   variance      = |147.20 - 159.68|           = 12.48
        assertThat(savedSummary.getTotalEligibleTransactions()).isEqualTo(4L);
        assertThat(savedSummary.getTotalExpectedPayout()).isEqualByComparingTo("159.68");
        assertThat(savedSummary.getTotalPaid()).isEqualByComparingTo("147.20");
        assertThat(savedSummary.getTotalVariance()).isEqualByComparingTo("12.48");
        assertThat(savedSummary.getMissingPayoutCount()).isEqualTo(1L);    // TX-3
        assertThat(savedSummary.getPartialPayoutCount()).isEqualTo(1L);    // TX-2
        assertThat(savedSummary.getOverpaymentCount()).isEqualTo(1L);      // TX-4
        assertThat(savedSummary.getIneligiblePayoutCount()).isEqualTo(0L); // all are SETTLED

        // Since job didn't exist yet, exceptions should be persisted
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ReconciliationException>> exCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(exceptionRepo, times(1)).saveAll(exCaptor.capture());

        List<ReconciliationException> savedExceptions = new ArrayList<>();
        exCaptor.getValue().forEach(savedExceptions::add);

        // Expect exceptions for TX-2 (PARTIAL), TX-3 (MISSING), TX-4 (OVERPAYMENT)
        assertThat(savedExceptions).hasSize(3);
        assertThat(savedExceptions.stream().map(ReconciliationException::getTransactionId))
                .containsExactlyInAnyOrder("TX-2", "TX-3", "TX-4");
        assertThat(savedExceptions.stream().map(ReconciliationException::getExceptionType).toList())
                .containsExactlyInAnyOrder(
                        ReconciliationException.ExceptionType.MISSING_PAYOUT,
                        ReconciliationException.ExceptionType.PARTIAL_PAYOUT,
                        ReconciliationException.ExceptionType.OVERPAYMENT
                );

        // Returned DTO via getters
        assertThat(jobDto).isNotNull();
        assertThat(jobDto.getStatus()).isEqualTo(ReconciliationJob.JobStatus.SUCCEEDED.name());
        assertThat(jobDto.getStartDate()).isEqualTo(start);
        assertThat(jobDto.getEndDate()).isEqualTo(end);

        // IMPORTANT: Don't assert "no more interactions" on transactionRepo/payoutRepo
        // because reconcile() intentionally calls payoutRepo.findById() multiple times per transaction.
        verifyNoMoreInteractions(exceptionRepo, summaryRepo, jobRepo);
    }

    @Test
    @DisplayName("run(): skips saving exceptions when a job for the same dates already exists; still saves summary and marks job SUCCEEDED")
    void run_skipsExceptions_whenJobAlreadyExists() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end   = LocalDate.of(2024, 2, 28);

        // Minimal scenario: one settled transaction with exact payout
        Transaction t = new Transaction();
        t.setTransactionId("EXACT-1");
        t.setAmount(new BigDecimal("100.00")); // expected 97.20
        t.setStatus(Transaction.Status.SETTLED);
        t.setSettlementDate(LocalDate.of(2024, 2, 10));

        Payout p = new Payout();
        p.setTransactionId("EXACT-1");
        p.setPayoutAmount(new BigDecimal("97.20"));

        when(jobRepo.existsByStartDateAndEndDate(start, end)).thenReturn(true);
        when(jobRepo.save(any(ReconciliationJob.class))).thenAnswer(invocation -> {
            ReconciliationJob j = invocation.getArgument(0);
            if (j.getId() == null) j.setId(2L);
            return j;
        });
        when(transactionRepo.findBySettlementDateBetween(start, end)).thenReturn(List.of(t));
        when(payoutRepo.findByPayoutDateBetween(start, end)).thenReturn(List.of(p));
        when(payoutRepo.findById("EXACT-1")).thenReturn(Optional.of(p));
        when(summaryRepo.save(any(ReconciliationSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RunRequestDto req = new RunRequestDto();
        req.setStartDate(start);
        req.setEndDate(end);

        JobDto jobDto = reconciliationService.run(req);

        // Verify the window fetch calls explicitly
        verify(transactionRepo, times(1)).findBySettlementDateBetween(start, end);
        verify(payoutRepo, times(1)).findByPayoutDateBetween(start, end);

        // Summary should still be saved
        verify(summaryRepo, times(1)).save(any(ReconciliationSummary.class));
        // Exceptions should NOT be saved because exists == true
        verify(exceptionRepo, never()).saveAll(anyList());

        // Job should be saved (created RUNNING, then updated SUCCEEDED)
        verify(jobRepo, atLeast(1)).save(any(ReconciliationJob.class));
        verify(jobRepo, times(1)).existsByStartDateAndEndDate(start, end);

        // Basic check on returned JobDto via getters
        assertThat(jobDto).isNotNull();
        assertThat(jobDto.getStatus()).isEqualTo(ReconciliationJob.JobStatus.SUCCEEDED.name());
        assertThat(jobDto.getStartDate()).isEqualTo(start);
        assertThat(jobDto.getEndDate()).isEqualTo(end);

        // Again, exclude transactionRepo/payoutRepo from "no more interactions"
        verifyNoMoreInteractions(exceptionRepo, summaryRepo, jobRepo);
    }

    @Test
    @DisplayName("findStatusById delegates to ReconciliationJobRepository")
    void findStatusById_delegatesToRepo() {
        ReconciliationJob job = new ReconciliationJob();
        job.setId(42L);
        job.setStatus(ReconciliationJob.JobStatus.SUCCEEDED);

        when(jobRepo.findJobStatusById(42L)).thenReturn(job);

        ReconciliationJob found = reconciliationService.findStatusById(42L);

        assertThat(found).isSameAs(job);
        verify(jobRepo, times(1)).findJobStatusById(42L);
        verifyNoMoreInteractions(jobRepo);
    }

    @Test
    @DisplayName("findReconciliationJobByDates delegates to existsByStartDateAndEndDate")
    void findReconciliationJobByDates_delegatesToRepo() {
        LocalDate start = LocalDate.of(2024, 3, 1);
        LocalDate end   = LocalDate.of(2024, 3, 31);

        when(jobRepo.existsByStartDateAndEndDate(start, end)).thenReturn(true);

        boolean exists = reconciliationService.findReconciliationJobByDates(start, end);

        assertThat(exists).isTrue();
        verify(jobRepo, times(1)).existsByStartDateAndEndDate(start, end);
        verifyNoMoreInteractions(jobRepo);
    }

    @Test
    @DisplayName("findReconciliationSummaryByJobId delegates to ReconciliationSummaryRepository")
    void findReconciliationSummaryByJobId_delegatesToRepo() {
        ReconciliationSummary summary = new ReconciliationSummary();
        summary.setJobId(7L);

        when(summaryRepo.findById(7L)).thenReturn(Optional.of(summary));

        Optional<ReconciliationSummary> result = reconciliationService.findReconciliationSummaryByJobId(7L);

        assertThat(result).isPresent().containsSame(summary);
        verify(summaryRepo, times(1)).findById(7L);
        verifyNoMoreInteractions(summaryRepo);
    }

    @Test
    @DisplayName("findReconciliationExceptionsByJobId currently returns repository.findAll()")
    void findReconciliationExceptionsByJobId_returnsAllFromRepo() {
        // Note: Implementation ignores the id and returns exceptionRepo.findAll()
        ReconciliationException e1 = new ReconciliationException();
        ReconciliationException e2 = new ReconciliationException();

        when(exceptionRepo.findAll()).thenReturn(List.of(e1, e2));

        List<ReconciliationException> exceptions = reconciliationService.findReconciliationExceptionsByJobId(123L);

        assertThat(exceptions).hasSize(2).containsExactly(e1, e2);
        verify(exceptionRepo, times(1)).findAll();
        verifyNoMoreInteractions(exceptionRepo);
    }

    @Test
    void triggerAsync_shouldCallRun() throws Exception {
        RunRequestDto req = new RunRequestDto();

        // 1. Extract the real target object from Spring’s proxy
        ReconciliationService target = AopTestUtils.getTargetObject(reconciliationService);

        // 2. Create a spy of the target
        ReconciliationService spy = spy(target);

        // 3. Replace the target inside the proxy
        Advised advised = (Advised) reconciliationService;
        advised.setTargetSource(new SingletonTargetSource(spy));

        // 4. Trigger async method
        reconciliationService.triggerAsync(req);

        // 5. Verify that run() was called asynchronously
        verify(spy, timeout(1500)).run(req);
    }


}