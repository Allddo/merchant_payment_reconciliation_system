package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.exception.ReconciliationException;
import com.capgemini.mprs.exception.ReconciliationExceptionRepository;
import com.capgemini.mprs.exception.ReconciliationExceptionService;
import com.capgemini.mprs.payout.Payout;
import com.capgemini.mprs.payout.PayoutRepository;
import com.capgemini.mprs.transaction.Transaction;
import com.capgemini.mprs.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepo;
    private final PayoutRepository payoutRepo;
    private final ReconciliationJobRepository jobRepo;
    private final ReconciliationSummaryRepository summaryRepo;
    private final ReconciliationExceptionRepository exceptionRepo;
    private final ReconciliationExceptionService reconciliationExceptionService;

    public ReconciliationJob.JobStatus findStatusById(Long id){
        return jobRepo.findJobStatusById(id);
    }

    public Optional<ReconciliationSummary> findReconciliationSummaryByJobId(Long id){
        return summaryRepo.findById(id);
    }

    public List<ReconciliationException> findReconciliationExceptionsByJobId(Long id){
        return exceptionRepo.findAll();
    }

    public JobDto run(RunRequest request) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        // Step 1: Create job (status RUNNING)
        ReconciliationJob job = createJob(start, end);

        // Step 2: Load all transactions in window
        List<Transaction> transactions =
                transactionRepo.findBySettlementDateBetween(start, end);

        // Step 3: Load all payouts in window
        List<Payout> payouts =
                payoutRepo.findByPayoutDateBetween(start, end);

        // Step 4: Perform reconciliation logic (matching, expected payout, exceptions)
        ReconciliationResult result = reconcile(job, transactions, payouts);

        // Step 5: Save summary & exception records
        summaryRepo.save(result.summary());
        exceptionRepo.saveAll(result.exceptions());

        // Step 6: Mark job complete
        job.setStatus(ReconciliationJob.JobStatus.SUCCEEDED);
        jobRepo.save(job);

        return JobDto.from(job);
    }

    private ReconciliationJob createJob(LocalDate start, LocalDate end){

        ReconciliationJob job = new ReconciliationJob();
        job.setStartDate(start);
        job.setEndDate(end);
        job.setStatus(ReconciliationJob.JobStatus.RUNNING);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        return jobRepo.save(job);

    }

    private ReconciliationResult reconcile(
            ReconciliationJob job,
            List<Transaction> transactions,
            List<Payout> payouts
    ) {

        Long missingPayoutCount = 0L;
        Long partialPayoutCount = 0L;
        Long overpaymentCount = 0L;
        Long ineligiblePayoutCount = 0L;

        for(Transaction t: transactions){

            BigDecimal expected = t.getAmount().subtract(t.getAmount().multiply(BigDecimal.valueOf(0.025))).subtract(BigDecimal.valueOf(0.30));
            BigDecimal actual = payoutRepo.findById(t.getTransactionId())
                    .map(Payout::getPayoutAmount)
                    .orElse(BigDecimal.ZERO);
            BigDecimal variance = expected.subtract(actual).abs();

            if(payoutRepo.findById(t.getTransactionId()) == null){
                reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, expected, ReconciliationException.ExceptionType.MISSING_PAYOUT, "Payment was missing for transaction: " + t.getTransactionId());
                missingPayoutCount++;
            }
            else if(expected.compareTo(actual) == 1){
                reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, variance, ReconciliationException.ExceptionType.PARTIAL_PAYOUT, "Payment was less than expected for transaction: " + t.getTransactionId());
                partialPayoutCount++;
            }
            else if(expected.compareTo(actual) == -1){
                reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, variance, ReconciliationException.ExceptionType.OVERPAYMENT, "Payment was more than expected for transaction: " + t.getTransactionId());
                overpaymentCount++;
            }

            if(payoutRepo.findById(t.getTransactionId()) != null && (t.getStatus() != Transaction.Status.SETTLED)){
                reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, variance, ReconciliationException.ExceptionType.INELIGIBLE_PAYOUT, "Payment exists for this transaction that is not eligible for payout: " + t.getTransactionId());
                ineligiblePayoutCount++;
            }

        }

        Set<String> settledTxIds = transactions.stream()
                .filter(tx -> "SETTLED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) // adjust if enum
                .map(Transaction::getTransactionId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // Expected = sum of SETTLED transaction amounts
        BigDecimal totalExpected = transactions.stream()
                .filter(tx -> tx.getStatus() == Transaction.Status.SETTLED)
                .map(Transaction::getAmount)
                .map(gross -> gross
                        .subtract(gross.multiply(BigDecimal.valueOf(0.025)))
                        .subtract(BigDecimal.valueOf(0.30))
                        .setScale(2, RoundingMode.HALF_UP)
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Actual = sum of all payout amounts in window
        BigDecimal totalActual = payouts.stream()
                .filter(p -> settledTxIds.contains(p.getTransactionId()))
                .map(Payout::getPayoutAmount)                              // BigDecimal
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = totalActual.subtract(totalExpected).abs();

        // Build summary
        ReconciliationSummary summary = new ReconciliationSummary();
        summary.setJob(job);
        summary.setTotalEligibleTransactions(
                (long) settledTxIds.size()
        );
        summary.setTotalExpectedPayout(totalExpected);
        summary.setTotalPaid(totalActual);
        summary.setTotalVariance(variance);
        summary.setMissingPayoutCount(missingPayoutCount);
        summary.setPartialPayoutCount(partialPayoutCount);
        summary.setOverpaymentCount(overpaymentCount);
        summary.setIneligiblePayoutCount(ineligiblePayoutCount);

        // No exceptions for now
        List<ReconciliationException> exceptions = List.of();

        return new ReconciliationResult(summary, exceptions);
    }


    @Async("reconExecutor")
    public void triggerAsync(RunRequest request) {
        run(request);
    }


}
