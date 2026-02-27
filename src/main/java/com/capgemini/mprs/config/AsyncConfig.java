package com.capgemini.mprs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// NEW: a simple executor for reconciliation jobs
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "reconExecutor")
    public Executor reconExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("recon-");
        ex.initialize();
        return ex;
    }
}