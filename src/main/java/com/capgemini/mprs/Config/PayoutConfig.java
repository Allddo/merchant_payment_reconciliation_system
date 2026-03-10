package com.capgemini.mprs.Config;

import com.capgemini.mprs.Dto.ProcessingResult;
import com.capgemini.mprs.Repository.PayoutRepository;
import com.capgemini.mprs.Entity.Payout;
import com.capgemini.mprs.Service.ExceptionEntityService;
import com.capgemini.mprs.Service.PayoutService;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class PayoutConfig {
    private final JobRepository jobRepository;
    private final PayoutService payoutService;
    private final PayoutRepository repository;
    private final ExceptionEntityService exceptionEntityService;

    @Bean
    public Step payoutStep(ItemReader<String> payoutReader,
                           ItemProcessor<String, ProcessingResult> payoutProcessor,
                           ItemWriter<ProcessingResult> payoutWriterSelector) {
        return new ChunkOrientedStepBuilder<String, ProcessingResult>("payoutJob", jobRepository, 100)
                .reader(payoutReader)
                .processor(payoutProcessor)
                .writer(payoutWriterSelector)
                //  .taskExecutor(new SimpleAsyncTaskExecutor()) // << enable multithreading
                .build();
    }

    @Bean
    public Job bulkPayoutIngestion(@Qualifier("payoutStep") Step payoutStep,
                                        JobRepository jobRepository) {
        return new JobBuilder("payoutIngestionJob", jobRepository)
                .start(payoutStep)
                .build();
    }


    @Bean
    @StepScope
    public FlatFileItemReader<String> payoutReader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<String>()
                .name("PayoutIngestion")
                .linesToSkip(1)
                .resource(new FileSystemResource(filePath))
                .lineMapper((line, lineNumber) -> line)
                .build();
    }
    @Bean
    @StepScope
    public ItemProcessor<String,ProcessingResult> payoutProcessor(){
        return payoutService::ingestPayout;
    }


    @Bean
    public ClassifierCompositeItemWriter<ProcessingResult> payoutWriterSelector(
            @Qualifier("payoutWriter")ItemWriter<ProcessingResult> payoutWriter,
            @Qualifier("payoutErrorWriter") ItemWriter<ProcessingResult> payoutErrorWriter
    ){
        ClassifierCompositeItemWriter<ProcessingResult> compositeItemWriter = new ClassifierCompositeItemWriter<>();
        compositeItemWriter.setClassifier(result -> {
            if (result.isError()){ // Checks invalid data, not differences.
                return payoutErrorWriter;
            }else {
                return payoutWriter;
            }
        });
        return compositeItemWriter;
    }

    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> payoutErrorWriter(){
        return items -> exceptionEntityService.saveAll(items.getItems().stream().map(ProcessingResult::getError).toList());
    }


    @Bean
    @StepScope
    public ItemWriter<ProcessingResult> payoutWriter(){
        return items -> repository.saveAll(items.getItems().stream().map(ProcessingResult::getPayout).toList());
    }
}


