package com.capgemini.mprs.transaction;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TransactionConfig {
    @Bean
    CommandLineRunner commandLineRunner(TransactionRepository repository)
    {
        return args -> {
            Transaction t1 = new Transaction(1,1.00,"AUTHORIZED","1/1/2026",1);
            Transaction t2 = new Transaction(2,1.50,"SETTLED","1/2/2026",3);
            repository.saveAll(List.of(t1,t2));
        };
    }
}
