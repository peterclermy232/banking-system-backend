package com.sacco.banking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class NotificationConfig {
    // This enables async processing for notifications
    // and scheduled tasks for cleanup
}