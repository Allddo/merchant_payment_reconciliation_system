package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.exception.ReconciliationException;
import com.capgemini.mprs.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public ResponseEntity<Void> run(@RequestBody RunRequest runRequest) {
        reconciliationService.triggerAsync(runRequest);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{jobId}")
    public ReconciliationJob.JobStatus getStatus(@PathVariable Long id){
        return reconciliationService.findStatusById(id);
    }

    @GetMapping("/{jobId}/summary")
    public Optional<ReconciliationSummary> getReconciliationSummaryByJobId(@PathVariable Long id){
        return reconciliationService.findReconciliationSummaryByJobId(id);
    }

    @GetMapping("/{jobId}/exceptions")
    public List<ReconciliationException> getReconciliationExceptionsByJobId(@PathVariable Long id){
        return reconciliationService.findReconciliationExceptionsByJobId(id);
    }

}
