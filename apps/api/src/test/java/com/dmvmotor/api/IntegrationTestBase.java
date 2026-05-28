package com.dmvmotor.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestBase.SyncAsyncTestConfig.class)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Run @Async jobs inline so post-completion side effects (the AI review
        // plan) finish within the test transaction boundary — no background
        // thread racing into the next test's truncateAll.
        registry.add("app.async.executor", () -> "sync");
    }

    @TestConfiguration
    static class SyncAsyncTestConfig {
        @Bean("aiTaskExecutor")
        @ConditionalOnProperty(name = "app.async.executor", havingValue = "sync")
        TaskExecutor syncAiTaskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
