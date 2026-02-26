package com.capgemini.mprs.payout;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payouts")
public class PayoutController {
    private final PayoutService service;
    private final JobLauncher jobLauncher;
    private final Job payoutJob;

     public PayoutController(PayoutService service,
                             JobLauncher jobLauncher,
                             @Qualifier("bulkPayoutIngestion") Job payoutJob) {
         this.service = service;
         this.jobLauncher = jobLauncher;
         this.payoutJob = payoutJob;
     }

     @PostMapping(path = "/bulk", consumes = "multipart/form-data")
     public String ingestPayout(@RequestParam MultipartFile file) throws Exception {
         Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
         Files.createDirectories(uploadDir); // validate that path exists or create it.
         String filename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
         Path destination = uploadDir.resolve(filename);
         Files.copy(file.getInputStream(), destination);
         JobParameters params = new JobParametersBuilder()
                 .addString("filePath", destination.toAbsolutePath().toString())
                 .addLong("timestamp", System.currentTimeMillis()) // required for unique job instances
                 .toJobParameters();
         jobLauncher.run(payoutJob,params);
         return "Job started";
     }


    @GetMapping
    public List<Payout> getPayout(Integer amount) {
        return service.getAll(amount);
    }


}
