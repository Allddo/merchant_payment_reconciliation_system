package com.capgemini.mprs.controllers;

import com.capgemini.mprs.custom_exceptions.JobNotFoundException;
import com.capgemini.mprs.entities.ReconciliationException;
import com.capgemini.mprs.entities.ReconciliationJob;
import com.capgemini.mprs.services.ReconciliationService;
import com.capgemini.mprs.entities.ReconciliationSummary;
import com.capgemini.mprs.dtos.RunRequestDto;
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
    public ResponseEntity<Void> run(@RequestBody RunRequestDto runRequest) {
        reconciliationService.triggerAsync(runRequest);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{jobId}")
    public ReconciliationJob getStatus(@PathVariable("jobId") Long id){
        ReconciliationJob rj = reconciliationService.findStatusById(id);

        if(rj == null){
            throw new JobNotFoundException("Job not found with given id: " + id);
        }
        return rj;
    }

    @GetMapping("/{jobId}/summary")
    public Optional<ReconciliationSummary> getReconciliationSummaryByJobId(@PathVariable("jobId") Long id){
        if(reconciliationService.findStatusById(id) == null){
            throw new JobNotFoundException("Job not found with given id: " + id);
        }
        return reconciliationService.findReconciliationSummaryByJobId(id);
    }

    @GetMapping("/{jobId}/exceptions")
    public List<ReconciliationException> getReconciliationExceptionsByJobId(@PathVariable("jobId") Long id){
        if(reconciliationService.findStatusById(id) == null){
            throw new JobNotFoundException("Job not found with given id: " + id);
        }
        return reconciliationService.findReconciliationExceptionsByJobId(id);
    }

}
