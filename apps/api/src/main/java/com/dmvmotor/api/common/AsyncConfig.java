package com.dmvmotor.api.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} so post-completion side jobs (e.g. the mock-exam AI
 * review plan) run off the request thread. The user's submit / exit response
 * returns immediately; the LLM call happens in the background.
 *
 * <p>Integration tests set {@code app.async.executor=sync} and supply a
 * synchronous {@code aiTaskExecutor} so the background job runs inline at commit
 * — deterministic, and no thread races into the next test's {@code truncateAll}.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("aiTaskExecutor")
    @ConditionalOnProperty(name = "app.async.executor", havingValue = "threadpool",
            matchIfMissing = true)
    public TaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-job-");
        executor.initialize();
        return executor;
    }
}
