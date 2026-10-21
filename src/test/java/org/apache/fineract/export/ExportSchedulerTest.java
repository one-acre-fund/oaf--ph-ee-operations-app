package org.apache.fineract.export;

import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 9, 0);
    private static final LocalDateTime CUTOFF = NOW.minusMinutes(2);

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ExportJobRepository repository = mock(ExportJobRepository.class);
    private final ExportWorker worker = mock(ExportWorker.class);
    private final ExportStorage storage = mock(ExportStorage.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final TenantServerConnection oaf = tenant("oaf");
    private final TenantServerConnection other = tenant("other");
    private ExportScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ExportScheduler(tenantContext, repository, worker, storage, executor, new ExportProperties(),
                Clock.fixed(Instant.parse("2026-10-20T09:00:00Z"), ZoneOffset.UTC));
        when(tenantContext.allTenants()).thenReturn(Arrays.asList(oaf, other));
        doAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        }).when(tenantContext).runAs(any(TenantServerConnection.class), any(Runnable.class));
        busyThreads(0, 0);
    }

    @Test
    @DisplayName("Requeues and abandons stale runs in every tenant, and fills free threads oldest job first")
    void recover() {
        when(repository.abandonStale(eq(CUTOFF), eq(3), eq(ExportScheduler.ABANDONED), anyString(), eq(NOW)))
                .thenReturn(1);
        when(repository.requeueStale(CUTOFF, 3)).thenReturn(2);
        when(repository.findWaiting(CUTOFF, PageRequest.of(0, 2))).thenReturn(Collections.singletonList(job("a")));
        when(repository.findWaiting(CUTOFF, PageRequest.of(0, 1))).thenReturn(Collections.singletonList(job("b")));

        scheduler.recover();

        verify(repository, times(2)).abandonStale(CUTOFF, 3, ExportScheduler.ABANDONED,
                "The export was interrupted 3 times; run it again", NOW);
        verify(worker).submit("oaf", "a");
        verify(worker).submit("other", "b");
    }

    @Test
    @DisplayName("Does not look for waiting jobs when this pod has no free thread")
    void noFreeThreads() {
        busyThreads(1, 1);

        scheduler.recover();

        verify(repository, times(2)).requeueStale(CUTOFF, 3);
        verify(repository, never()).findWaiting(any(), any());
    }

    @Test
    @DisplayName("Stops handing out jobs once the pool refuses one")
    void poolRefuses() {
        when(repository.findWaiting(eq(CUTOFF), any())).thenReturn(Arrays.asList(job("a"), job("b")));
        doThrow(new TaskRejectedException("full")).when(worker).submit("oaf", "a");

        scheduler.recover();

        verify(worker, never()).submit("oaf", "b");
        verify(worker, never()).submit(eq("other"), anyString());
    }

    @Test
    @DisplayName("A failing tenant is logged and does not stop the others")
    void failingTenant() {
        doThrow(new IllegalStateException("db down")).when(tenantContext).runAs(eq(oaf), any(Runnable.class));

        scheduler.recover();

        verify(repository).requeueStale(CUTOFF, 3);
    }

    @Test
    @DisplayName("Deletes expired files and marks the jobs EXPIRED; a failed delete is retried next run")
    void cleanUp() {
        when(repository.findByStatusAndExpiresAtBefore(ExportStatus.SUCCEEDED, NOW,
                PageRequest.of(0, ExportScheduler.CLEANUP_BATCH)))
                .thenReturn(Arrays.asList(job("a"), job("b")), Collections.emptyList());
        doThrow(new IllegalStateException("S3 down")).when(storage).delete("exports/oaf/a.csv");

        scheduler.cleanUp();

        verify(storage).delete("exports/oaf/b.csv");
        verify(repository).expire("b");
        verify(repository, never()).expire("a");
    }

    private void busyThreads(int active, int queued) {
        ThreadPoolExecutor pool = mock(ThreadPoolExecutor.class);
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        for (int i = 0; i < queued; i++) {
            queue.add(() -> { });
        }
        when(pool.getQueue()).thenReturn(queue);
        when(executor.getThreadPoolExecutor()).thenReturn(pool);
        when(executor.getMaxPoolSize()).thenReturn(2);
        when(executor.getActiveCount()).thenReturn(active);
    }

    private static TenantServerConnection tenant(String schema) {
        TenantServerConnection tenant = new TenantServerConnection();
        tenant.setSchemaName(schema);
        return tenant;
    }

    private static ExportJob job(String jobId) {
        ExportJob job = new ExportJob();
        job.setJobId(jobId);
        job.setStorageKey("exports/oaf/" + jobId + ".csv");
        return job;
    }
}
