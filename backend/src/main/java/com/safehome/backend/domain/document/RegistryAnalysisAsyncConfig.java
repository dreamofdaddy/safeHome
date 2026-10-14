package com.safehome.backend.domain.document;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class RegistryAnalysisAsyncConfig {
    @Bean(name = "registryAnalysisExecutor")
    public ThreadPoolTaskExecutor registryAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("registry-analysis-");
        executor.initialize();
        return executor;
    }
}
