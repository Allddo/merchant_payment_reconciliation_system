package com.capgemini.mprs.Config;

import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Repository.ReconciliationRepository;
import com.capgemini.mprs.Entity.Reconciliation;
import com.capgemini.mprs.Service.ExceptionEntityService;
import com.capgemini.mprs.Service.ReconciliationService;
import com.capgemini.mprs.Dto.ReconciliationSummary;
import com.capgemini.mprs.Entity.Transaction;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.ClassifierCompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.stream.*;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ReconciliationConfig {

    private final JobRepository jobRepository;
    private final ExceptionEntityService exceptionEntityService;
    private final ReconciliationService reconciliationService;
    private final EntityManagerFactory emf;

    @Bean
    public Step reconciliationStep(
            ItemReader<Transaction> reader,
            ItemProcessor<Transaction, ProcessingResult> processor,
            ItemWriter<ProcessingResult> writer
    )
    {
        return new ChunkOrientedStepBuilder<Transaction,ProcessingResult>("job",jobRepository,100)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
    @Bean
    public Job reconciliationJob(
            @Qualifier("reconciliationStep") Step reconciliationStep,
            JobRepository jobRepository
    ) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .start(reconciliationStep)
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        Long id = jobExecution.getJobParameters().getLong("reconciliationId");
                        if (id == null) return;

                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            reconciliationService.setStatus(id, "FINISHED");
                        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                            reconciliationService.setStatus(id, "FAILED");
                        }
                    }
                })
                .build();
    }



    @Bean
    @StepScope
    public JpaCursorItemReader<Transaction> reader(EntityManagerFactory emf) {
        return new JpaCursorItemReaderBuilder<Transaction>()
                .name("transactionJpaCursorItemReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t  WHERE t.status = 'SETTLED' ORDER BY t.transaction_id")
                .build();
    }



    @Bean
    @StepScope
    public ItemProcessor<Transaction, ProcessingResult> processor(@Value("#{jobParameters['reconciliationId']}") Long jobId){

        return item -> reconciliationService.processReconciliation(item,jobId);
    }

    @Bean
    public ClassifierCompositeItemWriter<ProcessingResult> writer(
            @Qualifier("ReconciliationWriter")ItemWriter<ProcessingResult> ReconciliationWriter,
            @Qualifier("ExceptionWriter") ItemWriter<ProcessingResult> ExceptionWriter
    ){
        ClassifierCompositeItemWriter<ProcessingResult> compositeItemWriter = new ClassifierCompositeItemWriter<>();
        compositeItemWriter.setClassifier(result -> {
                if (result.isError() && result.getSummary() == null){ // Checks invalid data, not differences.
                    return ExceptionWriter;
                }else {
                    return ReconciliationWriter;
                }
        });
        return compositeItemWriter;
    }

    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> ReconciliationWriter( @Value("#{jobParameters['reconciliationId']}") Long jobId){
        return items ->{
            Reconciliation r = reconciliationService.getReconciliationById(jobId);
            reconciliationService.updateSummary(r,items);
        };

    }

    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> ExceptionWriter( @Value("#{jobParameters['reconciliationId']}") Long jobId){
        return items ->{
            Long itemCount = (long) items.getItems().size();
            Reconciliation r = reconciliationService.getReconciliationById(jobId);
            r.setTotalExceptions(r.getTotalExceptions() + itemCount);
            reconciliationService.save(r);
            exceptionEntityService.saveAll(items.getItems().stream().map(ProcessingResult::getError).toList());


        };

    }

}

