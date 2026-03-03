package com.capgemini.mprs.Config;


import com.capgemini.mprs.Repository.TransactionRepository;
import com.capgemini.mprs.Entity.Transaction;
import com.capgemini.mprs.Service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class TransactionConfig {
    private final JobRepository jobRepository;
    private final TransactionService transactionService;
    private final TransactionRepository repository;
    @Bean
    public Step transactionStep(ItemReader<String> transactionReader,
                                ItemProcessor<String, Transaction> transactionProcessor,
                                ItemWriter<Transaction> transactionWriter)
    {
        return new ChunkOrientedStepBuilder<String,Transaction>("transactionJob",jobRepository,100)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
              //  .taskExecutor(new SimpleAsyncTaskExecutor()) // << enable multithreading
                .build();
    }
    @Bean
    public Job bulkIngestion(@Qualifier("transactionStep") Step transactionStep,
                             JobRepository jobRepository) {
        return new JobBuilder("transactionIngestionJob", jobRepository)
                .start(transactionStep)
                .build();
    }



    @Bean
    @StepScope
    public FlatFileItemReader<String> transactionReader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<String>()
                .name("TransactionIngestion")
                .linesToSkip(1)
                .resource(new FileSystemResource(filePath))
                .lineMapper((line, lineNumber) -> line)
                .build();
    }



    @Bean
    @StepScope
    public ItemProcessor<String,Transaction> transactionProcessor(){
        return transactionService::ingestTransaction;
    }

    @Bean
    @StepScope
    public ItemWriter<Transaction> transactionWriter(){
        return repository::saveAll;
    }


}
