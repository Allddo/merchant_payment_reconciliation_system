package com.capgemini.mprs.reconciliation;

import com.capgemini.mprs.transaction.Transaction;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class ReconciliationConfig {

    private final JobRepository jobRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationService reconciliationService;
    private final EntityManagerFactory emf;

    @Bean
    public Step reconciliationStep(
            ItemReader<Transaction> reader,
            ItemProcessor<Transaction, ReconciliationSummary> processor,
            ItemWriter<ReconciliationSummary> writer
    )
    {
        return new ChunkOrientedStepBuilder<Transaction,ReconciliationSummary>("job",jobRepository,100)
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
    public ItemProcessor<Transaction,ReconciliationSummary> processor(@Value("#{jobParameters['reconciliationId']}") Long jobId){

        return item -> reconciliationService.processReconciliation(item,jobId);
    }

    @Bean
    @StepScope
    public ItemWriter<ReconciliationSummary> writer( @Value("#{jobParameters['reconciliationId']}") Long jobId){
        return items ->{
            Reconciliation r = reconciliationService.getReconciliationById(jobId);
            reconciliationService.updateSummary(r,items);
        };

    }

}

