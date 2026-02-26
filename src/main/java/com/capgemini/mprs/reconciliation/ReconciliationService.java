package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.exception.ExceptionEntityService;
import com.capgemini.mprs.payout.PayoutRepository;
import com.capgemini.mprs.transaction.Transaction;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ReconciliationService {
    private final ReconciliationRepository reconciliationRepository;
    private final PayoutRepository payoutRepository;
    private final ExceptionEntityService exceptionEntityService;

    @Autowired
    public ReconciliationService(ReconciliationRepository reconciliationRepository, PayoutRepository payoutRepository, ExceptionEntityService exceptionEntityService) {
        this.reconciliationRepository = reconciliationRepository;
        this.payoutRepository = payoutRepository;
        this.exceptionEntityService = exceptionEntityService;

    }

    @Transactional
    public void addReconciliation(Reconciliation reconciliation) {
        reconciliationRepository.save(reconciliation);
    }

    public ReconciliationSummary processReconciliation(Transaction transaction, Long jobId) {
        if(payoutRepository.findById(transaction.getTransaction_id()).isEmpty()){
            exceptionEntityService.createException(404, "Job Not Found", "No Payout for transaction id: " + transaction.getTransaction_id(), "/api/v1/reconciliations/" + jobId);
            return new ReconciliationSummary(transaction.getTransaction_id(), BigDecimal.ZERO, BigDecimal.ZERO, true); }
        BigDecimal payoutAmount = payoutRepository.findById(transaction.getTransaction_id()).get().getPayout_amount();

        BigDecimal estimatePayout = transaction.getAmount().subtract
                        (((transaction.getAmount().multiply(BigDecimal.valueOf(.025))).add(BigDecimal.valueOf(.30))))
                .setScale(2, RoundingMode.HALF_UP);
        boolean isException = false;
        BigDecimal difference = (payoutAmount.subtract(estimatePayout)).setScale(2, RoundingMode.HALF_UP).abs();
        if (estimatePayout.compareTo(payoutAmount) > 0) {
            isException = true;
            exceptionEntityService.createException(400, "Invalid Field", "Underpaid by $" + difference, "/api/v1/reconciliations/" + jobId);
        }
        else if (estimatePayout.compareTo(payoutAmount) < 0) {
            isException = true;
            exceptionEntityService.createException(400, "Invalid Field", "Overpaid by $" + difference, "/api/v1/reconciliations/" + jobId);
        }
        return new ReconciliationSummary(transaction.getTransaction_id(), payoutAmount, estimatePayout, isException);
    }

    public Reconciliation getReconciliationById(Long jobId) {
        return reconciliationRepository.findById(jobId).orElse(null);
    }

    public void updateSummary(Reconciliation r, Chunk<? extends ReconciliationSummary> items) {
        for (ReconciliationSummary item : items) {
            r.setTotalEligibleTransactions(r.getTotalEligibleTransactions() + 1);
            r.setTotalPaid(r.getTotalPaid().add(item.getPayout()));
            r.setTotalExpectedPayout(r.getTotalExpectedPayout().add(item.getExpectedPayout()));
            r.setTotalVariance(r.getTotalPaid().subtract(r.getTotalExpectedPayout()).abs());
            if (item.isException()) {
                r.setTotalExceptions(r.getTotalExceptions() + 1);
            }
        }
        reconciliationRepository.save(r);
    }
    @Transactional
    public void setStatus(Long id, String status) {
        reconciliationRepository.findById(id).ifPresent(r -> r.setStatus(status));
    }
}

