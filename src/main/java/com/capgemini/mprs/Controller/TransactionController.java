package com.capgemini.mprs.Controller;

import com.capgemini.mprs.Entity.ExceptionEntity;
import com.capgemini.mprs.Entity.Transaction;
import com.capgemini.mprs.Service.TransactionService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("api/v1/transactions/")
public class TransactionController {
    private final TransactionService service;
    private final JobLauncher jobLauncher;
    private final Job bulkIngestion;
    public TransactionController(TransactionService service,
                                 JobLauncher jobLauncher,
                                 @Qualifier("bulkIngestion") Job bulkIngestion) {
        this.service = service;
        this.jobLauncher = jobLauncher;
        this.bulkIngestion = bulkIngestion;
    }

    // Get transaction by an id of some sort change to pageable for website ui
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_ANALYST')")
    public List<Transaction> getTransaction(@RequestParam Integer amount) {
        if(amount < 0){
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if(amount > service.getCount()){
            throw new IllegalArgumentException("Amount cannot be greater than total payouts");
        }
        return service.getAll(amount);
    }


    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public String runJob(@RequestParam MultipartFile file) throws Exception {
        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir); // validate that path exists or create it.
        String filename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        if(!filename.toLowerCase().endsWith(".csv")){
            throw new IllegalArgumentException("Invalid file format, only CSV files are accepted");
        }

        Path destination = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), destination);
        JobParameters params = new JobParametersBuilder()
                .addString("filePath", destination.toAbsolutePath().toString())
                .addLong("timestamp", System.currentTimeMillis()) // required for unique job instances
                .toJobParameters();
        jobLauncher.run(bulkIngestion,params);
        return "Transaction Job Started Successfully" ;
    }
    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_ANALYST')")
    public List<ExceptionEntity>getTransactionExceptions(){
        return service.getExceptions();
    }

}
