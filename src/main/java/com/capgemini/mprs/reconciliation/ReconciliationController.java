package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.exception.ExceptionEntity;
import com.capgemini.mprs.exception.ExceptionEntityService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
       public String getJobStatus(@PathVariable Long job_id){
        if(service.getReconciliationById(job_id) != null){
            return "Job ID: " + job_id.toString()+ " Status: " + service.getReconciliationById(job_id).getStatus();
        }
        return "oh no";
    }

    @GetMapping("/{job_id}/exceptions") //Can return page in the future?
    public List<ExceptionEntity> getJobExceptions(@PathVariable Long job_id, Pageable pageable){
        return exceptionEntityService.selectAllByPath("/api/v1/reconciliations/" + job_id, pageable);
    }

    @GetMapping("/{job_id}/summary")
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
        return "Job Not Found";
    }
}
