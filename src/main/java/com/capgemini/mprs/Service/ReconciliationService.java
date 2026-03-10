package com.capgemini.mprs.Service;

import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Entity.Reconciliation;
import com.capgemini.mprs.Repository.ReconciliationRepository;
import com.capgemini.mprs.Repository.PayoutRepository;
import com.capgemini.mprs.Entity.Transaction;
import com.capgemini.mprs.Dto.ReconciliationSummary;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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


    public void addReconciliation(Reconciliation reconciliation) {
        reconciliationRepository.save(reconciliation);
    }

    public ProcessingResult processReconciliation(Transaction transaction, Long jobId) {
        ProcessingResult r = new ProcessingResult();
        if(payoutRepository.findById(transaction.getTransaction_id()).isEmpty()){
            r.setError(new ExceptionEntity(404, "Not Found", "No Payout for transaction id: " + transaction.getTransaction_id(), "/api/v1/reconciliations/" + jobId));
            return r;
        }
        BigDecimal payoutAmount = payoutRepository.findById(transaction.getTransaction_id()).get().getPayout_amount();

        BigDecimal estimatePayout = transaction.getAmount().subtract
                        (((transaction.getAmount().multiply(BigDecimal.valueOf(.025))).add(BigDecimal.valueOf(.30))))
                .setScale(2, RoundingMode.HALF_UP);

        r.setSummary(new ReconciliationSummary(transaction.getTransaction_id(), payoutAmount, estimatePayout));

        BigDecimal difference = (payoutAmount.subtract(estimatePayout)).setScale(2, RoundingMode.HALF_UP).abs();
        if (estimatePayout.compareTo(payoutAmount) > 0) {
            r.setError(new ExceptionEntity(400, "Difference",  "Underpaid by $" + difference, "/api/v1/reconciliations/" + jobId));
        }
        else if (estimatePayout.compareTo(payoutAmount) < 0) {
            r.setError(new ExceptionEntity(400, "Difference",  "Overpaid by $" + difference, "/api/v1/reconciliations/" + jobId));
        }
        return r;
    }

    public Reconciliation getReconciliationById(Long jobId) {
        return reconciliationRepository.findById(jobId).orElse(null);
    }

    public void updateSummary(Reconciliation r, Chunk<? extends ProcessingResult> items) {
        List<ExceptionEntity> exceptions = new ArrayList<>();
        for (ProcessingResult item : items) {
            r.setTotalEligibleTransactions(r.getTotalEligibleTransactions() + 1);
            r.setTotalPaid(r.getTotalPaid().add(item.getSummary().getPayout()));
            r.setTotalExpectedPayout(r.getTotalExpectedPayout().add(item.getSummary().getExpectedPayout()));
            r.setTotalVariance(r.getTotalPaid().subtract(r.getTotalExpectedPayout()).abs());
            if (item.isError()) {
                r.setTotalExceptions(r.getTotalExceptions() + 1);
                exceptions.add(item.getError());
            }
        }
        if(!exceptions.isEmpty())
        {
            exceptionEntityService.saveAll(exceptions);
        }
        reconciliationRepository.save(r);
    }


    @Transactional
    public void setStatus(Long id, String status) {
        reconciliationRepository.findById(id).ifPresent(r -> r.setStatus(status));
    }

    public void save(Reconciliation r) {
        reconciliationRepository.save(r);
    }
}

