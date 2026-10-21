package org.apache.fineract.export;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;

@Configuration
@EnableScheduling
public class ExportExecutorConfig {

    /**
     * Bounded pool, so exports cannot take more threads (and DB connections) than configured, whatever the demand.
     */
    @Bean(name = "exportExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor exportExecutor(ExportProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getWorkerThreads());
        executor.setMaxPoolSize(properties.getWorkerThreads());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("export-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "exportClock")
    public Clock exportClock() {
        return Clock.systemUTC();
    }
}
