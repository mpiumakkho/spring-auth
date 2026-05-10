package com.mp.core.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async executor for ApplicationEvent listeners that fan out audit + notification
 * writes off the main request thread. Single dedicated pool keeps observability
 * predictable; swap for a message broker (Kafka/RabbitMQ) when going multi-process.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${core.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${core.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${core.async.queue-capacity:200}")
    private int queueCapacity;

    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
