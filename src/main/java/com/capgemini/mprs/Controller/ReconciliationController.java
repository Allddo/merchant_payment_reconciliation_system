package com.capgemini.mprs.Controller;

import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Service.ExceptionEntityService;
import com.capgemini.mprs.Entity.Reconciliation;
import com.capgemini.mprs.Service.ReconciliationService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliations")
public class ReconciliationController {
    private final ReconciliationService service;
    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;
    private final ExceptionEntityService exceptionEntityService;

    public ReconciliationController(ReconciliationService service,
                                    JobLauncher jobLauncher,
                                    @Qualifier("reconciliationJob") Job reconciliationJob, ExceptionEntityService exceptionEntityService) {
        this.service = service;
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
        this.exceptionEntityService = exceptionEntityService;
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public String runJob() throws Exception {
        Reconciliation r = new Reconciliation();
        service.addReconciliation(r);
        JobParameters params = new JobParametersBuilder()
                .addLong("reconciliationId", r.getJob_id())
                .toJobParameters();
        jobLauncher.run(reconciliationJob,params);
        return "Job started with id: " + r.getJob_id();
    }

     @GetMapping("/{job_id}")
     @PreAuthorize("hasAnyRole('ADMIN','FINANCE_ANALYST')")
       public String getJobStatus(@PathVariable Long job_id){
        if(service.getReconciliationById(job_id) != null){
            return "Job ID: " + job_id.toString()+ " Status: " + service.getReconciliationById(job_id).getStatus();
        }
        else {
            throw new IllegalArgumentException("Job not found");
        }
    }

    @GetMapping("/{job_id}/exceptions")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_ANALYST')")//Can return page in the future?
    public List<ExceptionEntity> getJobExceptions(@PathVariable Long job_id, Pageable pageable){
        return exceptionEntityService.selectAllByPath("/api/v1/reconciliations/" + job_id, pageable);
    }

    @GetMapping("/{job_id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_ANALYST')")
    public String getJobSummary(@PathVariable Long job_id){
        if(service.getReconciliationById(job_id) != null){
            StringBuilder s = new StringBuilder();
            s.append("Job Id: ").append(job_id).append("\n");
            s.append("Total Eligible Transactions: ").append(service.getReconciliationById(job_id).getTotalEligibleTransactions()).append("\n");
            s.append("Total Expected Payout: $").append(service.getReconciliationById(job_id).getTotalExpectedPayout()).append("\n");
            s.append("Total Paid: $").append(service.getReconciliationById(job_id).getTotalPaid()).append("\n");
            s.append("Total Variance: $").append(service.getReconciliationById(job_id).getTotalVariance()).append("\n");
            s.append("Total Exceptions: ").append(service.getReconciliationById(job_id).getTotalExceptions()).append("\n");
            return s.toString();
        }
        else{
            throw new IllegalArgumentException("Job not found");
        }
    }
}
