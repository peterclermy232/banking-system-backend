package com.sacco.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SaccoBankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SaccoBankingApplication.class, args);
    }
}