package org.apache.fineract.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportExecutorConfigTest {

    @Test
    @DisplayName("The export pool is bounded by the configured threads and queue")
    void boundedPool() {
        ExportProperties properties = new ExportProperties();
        properties.setWorkerThreads(3);
        properties.setQueueCapacity(5);
        ExportExecutorConfig config = new ExportExecutorConfig();

        ThreadPoolTaskExecutor executor = config.exportExecutor(properties);
        try {
            assertEquals(3, executor.getCorePoolSize());
            assertEquals(3, executor.getMaxPoolSize());
            assertEquals(5, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
            assertEquals("export-", executor.getThreadNamePrefix());
            assertEquals(ZoneOffset.UTC, config.exportClock().getZone());
        } finally {
            executor.shutdown();
        }
    }
}
