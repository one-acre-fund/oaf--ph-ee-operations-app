package org.apache.fineract.export;

import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Background housekeeping, on every pod and for every tenant. Every step is a conditional UPDATE or an idempotent
 * delete, so pods running it at the same time do not need a lock.
 * <ul>
 *     <li>recover: takes back RUNNING jobs whose heartbeat stopped (pod died) and gives waiting QUEUED jobs to
 *     this pod's free threads, so no job depends on one pod's memory;</li>
 *     <li>clean up: deletes the files of expired exports from storage.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "export.async.enabled", havingValue = "true")
public class ExportScheduler {

    static final String ABANDONED = "ABANDONED";
    static final int CLEANUP_BATCH = 100;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TenantContext tenantContext;
    private final ExportJobRepository jobRepository;
    private final ExportWorker worker;
    private final ExportStorage storage;
    private final ThreadPoolTaskExecutor executor;
    private final ExportProperties properties;
    private final Clock clock;

    @SuppressWarnings("java:S107") // collaborators of the scheduler, injected by Spring
    public ExportScheduler(TenantContext tenantContext, ExportJobRepository jobRepository, ExportWorker worker,
                           ExportStorage storage, @Qualifier("exportExecutor") ThreadPoolTaskExecutor executor,
                           ExportProperties properties, @Qualifier("exportClock") Clock clock) {
        this.tenantContext = tenantContext;
        this.jobRepository = jobRepository;
        this.worker = worker;
        this.storage = storage;
        this.executor = executor;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${export.recovery-interval:PT15S}",
            initialDelayString = "${export.recovery-interval:PT15S}")
    public void recover() {
        AtomicInteger freeThreads = new AtomicInteger(freeThreads());
        forEachTenant("recovery", tenant -> recover(tenant.getSchemaName(), freeThreads));
    }

    @Scheduled(fixedDelayString = "${export.cleanup-interval:PT10M}", initialDelayString = "PT1M")
    public void cleanUp() {
        forEachTenant("cleanup", tenant -> deleteExpiredFiles());
    }

    void recover(String tenant, AtomicInteger freeThreads) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(properties.getStaleAfter());
        int abandoned = jobRepository.abandonStale(cutoff, properties.getMaxAttempts(), ABANDONED,
                "The export was interrupted " + properties.getMaxAttempts() + " times; run it again", now);
        // keeps the stale heartbeat, so the requeued jobs are picked up below without waiting
        int requeued = jobRepository.requeueStale(cutoff, properties.getMaxAttempts());
        if (abandoned + requeued > 0) {
            logger.warn("Tenant {}: {} interrupted exports requeued, {} abandoned", tenant, requeued, abandoned);
        }
        if (freeThreads.get() <= 0) {
            return;
        }
        List<ExportJob> waiting = jobRepository.findWaiting(cutoff, PageRequest.of(0, freeThreads.get()));
        for (ExportJob job : waiting) {
            try {
                worker.submit(tenant, job.getJobId());
                freeThreads.decrementAndGet();
            } catch (TaskRejectedException e) {
                freeThreads.set(0);
                return;
            }
        }
    }

    void deleteExpiredFiles() {
        List<ExportJob> expired = jobRepository.findByStatusAndExpiresAtBefore(ExportStatus.SUCCEEDED,
                LocalDateTime.now(clock), PageRequest.of(0, CLEANUP_BATCH));
        for (ExportJob job : expired) {
            try {
                storage.delete(job.getStorageKey());
                jobRepository.expire(job.getJobId());
            } catch (RuntimeException e) {
                logger.warn("Export {}: could not delete the expired file; retrying on the next run", job.getJobId(),
                        e);
            }
        }
    }

    private int freeThreads() {
        return executor.getMaxPoolSize() - executor.getActiveCount()
                - executor.getThreadPoolExecutor().getQueue().size();
    }

    private void forEachTenant(String task, Consumer<TenantServerConnection> work) {
        for (TenantServerConnection tenant : tenantContext.allTenants()) {
            try {
                tenantContext.runAs(tenant, () -> work.accept(tenant));
            } catch (RuntimeException e) {
                logger.error("Export {} failed for tenant {}", task, tenant.getSchemaName(), e);
            }
        }
    }
}
