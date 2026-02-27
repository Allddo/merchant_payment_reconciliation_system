package com.capgemini.mprs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MprsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MprsApplication.class, args);
    }

}