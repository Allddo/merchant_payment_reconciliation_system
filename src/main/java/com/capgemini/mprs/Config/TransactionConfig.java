package com.capgemini.mprs.Config;


import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Repository.TransactionRepository;
import com.capgemini.mprs.Entity.Transaction;
import com.capgemini.mprs.Service.ExceptionEntityService;
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
import org.springframework.batch.infrastructure.item.support.ClassifierCompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import java.util.stream.*;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class TransactionConfig {
    private final JobRepository jobRepository;
    private final TransactionService transactionService;
    private final TransactionRepository repository;
    private final ExceptionEntityService exceptionEntityService;
    @Bean
    public Step transactionStep(ItemReader<String> transactionReader,
                                ItemProcessor<String, ProcessingResult> transactionProcessor,
                                ItemWriter<ProcessingResult> transactionWriterSelector)
    {
        return new ChunkOrientedStepBuilder<String,ProcessingResult>("transactionJob",jobRepository,100)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriterSelector)
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
    public ItemProcessor<String,ProcessingResult> transactionProcessor(){
        return transactionService::ingestTransaction;
    }

    @Bean
    public ClassifierCompositeItemWriter<ProcessingResult> transactionWriterSelector(
            @Qualifier("transactionWriter")ItemWriter<ProcessingResult> transactionWriter,
            @Qualifier("transactionErrorWriter") ItemWriter<ProcessingResult> transactionErrorWriter
    ){
        ClassifierCompositeItemWriter<ProcessingResult> compositeItemWriter = new ClassifierCompositeItemWriter<>();
        compositeItemWriter.setClassifier(result -> {
            if (result.isError()){ // Checks invalid data, not differences.
                return transactionErrorWriter;
            }else {
                return transactionWriter;
            }
        });
        return compositeItemWriter;
    }

    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> transactionErrorWriter(){
        return items -> exceptionEntityService.saveAll(items.getItems().stream().map(ProcessingResult::getError).toList());
    }


    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> transactionWriter(){
        return items -> repository.saveAll(items.getItems().stream().map(ProcessingResult::getTransaction).toList());
    }


}
