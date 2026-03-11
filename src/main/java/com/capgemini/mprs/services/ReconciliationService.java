package com.capgemini.mprs.services;

import com.capgemini.mprs.dtos.JobDto;
import com.capgemini.mprs.dtos.ReconciliationResultDto;
import com.capgemini.mprs.dtos.RunRequestDto;
import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.entities.ReconciliationSummary;
import com.capgemini.mprs.repositories.ReconciliationExceptionRepository;
import com.capgemini.mprs.entities.Payout;
import com.capgemini.mprs.repositories.PayoutRepository;
import com.capgemini.mprs.entities.Transaction;
import com.capgemini.mprs.repositories.ReconciliationJobRepository;
import com.capgemini.mprs.repositories.ReconciliationSummaryRepository;
import com.capgemini.mprs.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepo;
    private final PayoutRepository payoutRepo;
    private final ReconciliationJobRepository jobRepo;
    private final ReconciliationSummaryRepository summaryRepo;
    private final ReconciliationExceptionRepository exceptionRepo;
    private final ReconciliationExceptionService reconciliationExceptionService;

    public ReconciliationJob findStatusById(Long id){
        return jobRepo.findJobStatusById(id);
    }

    public boolean findReconciliationJobByDates(LocalDate start, LocalDate end){
        return jobRepo.existsByStartDateAndEndDate(start, end);
    }

    public Optional<ReconciliationSummary> findReconciliationSummaryByJobId(Long id){
        return summaryRepo.findById(id);
    }

    public List<ReconciliationException> findReconciliationExceptionsByJobId(Long id){
        return exceptionRepo.findAll();
    }

    public JobDto run(RunRequestDto request) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        boolean exists = findReconciliationJobByDates(start, end);

        // Step 1: Create job (status RUNNING)
        ReconciliationJob job = createJob(start, end);

        // Step 2: Load all transactions in window
        List<Transaction> transactions =
                transactionRepo.findBySettlementDateBetween(start, end);

        // Step 3: Load all payouts in window
        List<Payout> payouts =
                payoutRepo.findByPayoutDateBetween(start, end);

        // Step 4: Perform reconciliation logic (matching, expected payout, exceptions)
        ReconciliationResultDto result = reconcile(job, transactions, payouts);

        // Step 5: Save summary & exception records
        summaryRepo.save(result.summary());

        if(!exists){
            exceptionRepo.saveAll(result.exceptions());
            System.out.println("false@@");
        }
        System.out.println("true@@");

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

    private ReconciliationResultDto reconcile(
            ReconciliationJob job,
            List<Transaction> transactions,
            List<Payout> payouts
    ) {
        Long missingPayoutCount = 0L;
        Long partialPayoutCount = 0L;
        Long overpaymentCount = 0L;
        Long ineligiblePayoutCount = 0L;

        Set<String> settledTxIds = transactions.stream()
                .filter(tx -> "SETTLED".equalsIgnoreCase(String.valueOf(tx.getStatus()))) // adjust if enum
                .map(Transaction::getTransactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

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
                .map(Payout::getPayoutAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = totalActual.subtract(totalExpected).abs();

        List<ReconciliationException> exceptions = new ArrayList<>();

        for(Transaction t: transactions){

            BigDecimal expected = t.getAmount().subtract(t.getAmount().multiply(BigDecimal.valueOf(0.025))).subtract(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal actual = payoutRepo.findById(t.getTransactionId())
                    .map(Payout::getPayoutAmount)
                    .orElse(BigDecimal.ZERO);
            BigDecimal varianceIndividual = expected.subtract(actual).abs();

            if(payoutRepo.findById(t.getTransactionId()) != null && (t.getStatus() != Transaction.Status.SETTLED)){
                exceptions.add(new ReconciliationException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.INELIGIBLE_PAYOUT, "Payment exists for this transaction that is not eligible for payout: " + t.getTransactionId()));
                //reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.INELIGIBLE_PAYOUT, "Payment exists for this transaction that is not eligible for payout: " + t.getTransactionId());
                ineligiblePayoutCount++;
                //throw new IllegalArgumentException("Payment exists for this transaction that is not eligible for payout: " + t.getTransactionId());
            }

            if(payoutRepo.findById(t.getTransactionId()).isEmpty()){
                exceptions.add(new ReconciliationException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.MISSING_PAYOUT, "Payment was missing for transaction: " + t.getTransactionId()));
                //reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.MISSING_PAYOUT, "Payment was missing for transaction: " + t.getTransactionId());
                missingPayoutCount++;
                //throw new IllegalArgumentException("Payment was missing for transaction: " + t.getTransactionId());
            }
            else if(expected.compareTo(actual) == 1){
                //System.out.println("Ex: " + expected + "---> actual: " + actual);
                exceptions.add(new ReconciliationException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.PARTIAL_PAYOUT, "Payment was less than expected for transaction: " + t.getTransactionId()));
                //reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.PARTIAL_PAYOUT, "Payment was less than expected for transaction: " + t.getTransactionId());
                partialPayoutCount++;
                //throw new IllegalArgumentException("Payment was less than expected for transaction: " + t.getTransactionId());
            }
            else if(expected.compareTo(actual) == -1){
                exceptions.add(new ReconciliationException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.OVERPAYMENT, "Payment was more than expected for transaction: " + t.getTransactionId()));
                //reconciliationExceptionService.createException(job, t.getTransactionId(), expected, actual, varianceIndividual, ReconciliationException.ExceptionType.OVERPAYMENT, "Payment was more than expected for transaction: " + t.getTransactionId());
                overpaymentCount++;
                //throw new IllegalArgumentException("Payment was more than expected for transaction: " + t.getTransactionId());
            }

        }

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

        return new ReconciliationResultDto(summary, exceptions);
    }

    @Transactional
    @Async("reconExecutor")
    public void triggerAsync(RunRequestDto request) {
        run(request);
    }


}
