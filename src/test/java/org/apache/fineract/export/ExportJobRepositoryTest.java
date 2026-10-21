package org.apache.fineract.export;

import org.apache.fineract.export.read.H2Entities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Runs the repository's JPQL against H2 through a real Spring Data proxy, with the same transaction handling as in
 * the application.
 */
class ExportJobRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 9, 0);
    private static final LocalDateTime CUTOFF = NOW.minusMinutes(2);

    private static EntityManagerFactory factory;
    private static ExportJobRepository repository;

    @BeforeAll
    static void startDatabase() {
        factory = H2Entities.create("export_jobs");
        JpaTransactionManager transactions = new JpaTransactionManager(factory);
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory));
        repositories.addRepositoryProxyPostProcessor((proxy, info) -> proxy.addAdvice(
                new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource())));
        repository = repositories.getRepository(ExportJobRepository.class);
    }

    @AfterAll
    static void stopDatabase() {
        factory.close();
    }

    @BeforeEach
    void emptyTable() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Only one claim wins, and only with the next attempt number; it starts the heartbeat")
    void claim() {
        save("a", ExportStatus.QUEUED, 0, null);
        save("requeued", ExportStatus.QUEUED, 1, null);

        assertEquals(1, repository.claim("a", 1, NOW));
        assertEquals(0, repository.claim("a", 1, NOW));
        assertEquals(0, repository.claim("requeued", 1, NOW));
        assertEquals(1, repository.claim("requeued", 2, NOW));

        ExportJob job = load("a");
        assertEquals(ExportStatus.RUNNING, job.getStatus());
        assertEquals(1, job.getAttempts());
        assertEquals(NOW, job.getStartedAt());
        assertEquals(NOW, job.getHeartbeatAt());
    }

    @Test
    @DisplayName("Heartbeat, success and failure only apply to the run holding the job")
    void runMustHoldTheJob() {
        save("a", ExportStatus.RUNNING, 2, CUTOFF);
        save("b", ExportStatus.RUNNING, 1, CUTOFF);

        assertEquals(0, repository.heartbeat("a", 1, NOW));
        assertEquals(1, repository.heartbeat("a", 2, NOW));
        assertEquals(0, repository.succeed("a", 1, 5, 50, "k", NOW, NOW.plusDays(1)));
        assertEquals(1, repository.succeed("a", 2, 5, 50, "k", NOW, NOW.plusDays(1)));
        assertEquals(0, repository.fail("a", 2, "X", "late", NOW));
        assertEquals(1, repository.fail("b", 1, "READ_FAILED", "failed", NOW));

        ExportJob a = load("a");
        assertEquals(ExportStatus.SUCCEEDED, a.getStatus());
        assertEquals(NOW, a.getHeartbeatAt());
        assertEquals(Long.valueOf(5), a.getRowCount());
        assertEquals(Long.valueOf(50), a.getFileSizeBytes());
        assertEquals("k", a.getStorageKey());
        assertEquals(NOW.plusDays(1), a.getExpiresAt());
        assertNull(a.getErrorCode());
        ExportJob b = load("b");
        assertEquals(ExportStatus.FAILED, b.getStatus());
        assertEquals("READ_FAILED", b.getErrorCode());
        assertEquals(NOW, b.getCompletedAt());
    }

    @Test
    @DisplayName("Stale runs are requeued while attempts are left, then abandoned; live runs are left alone")
    void staleRuns() {
        save("retry", ExportStatus.RUNNING, 1, CUTOFF.minusSeconds(1));
        save("give-up", ExportStatus.RUNNING, 3, CUTOFF.minusSeconds(1));
        ExportJob handedBackTwice = job("handed-back", ExportStatus.RUNNING, 3, CUTOFF.minusSeconds(1));
        handedBackTwice.setReleases(2);
        repository.save(handedBackTwice);
        save("alive", ExportStatus.RUNNING, 1, CUTOFF);

        assertEquals(1, repository.abandonStale(CUTOFF, 3, "ABANDONED", "interrupted", NOW));
        assertEquals(2, repository.requeueStale(CUTOFF, 3));

        assertEquals(ExportStatus.QUEUED, load("retry").getStatus());
        assertEquals(CUTOFF.minusSeconds(1), load("retry").getHeartbeatAt());
        assertEquals(ExportStatus.QUEUED, load("handed-back").getStatus());
        assertEquals(ExportStatus.FAILED, load("give-up").getStatus());
        assertEquals("ABANDONED", load("give-up").getErrorCode());
        assertEquals(ExportStatus.RUNNING, load("alive").getStatus());
    }

    @Test
    @DisplayName("A shutting-down pod hands its run back without counting it, and expedites its waiting jobs")
    void handBack() {
        save("running", ExportStatus.RUNNING, 2, NOW);
        save("waiting", ExportStatus.QUEUED, 0, NOW);
        save("still-running", ExportStatus.RUNNING, 1, NOW);

        assertEquals(0, repository.release("running", 1));
        assertEquals(1, repository.release("running", 2));
        assertEquals(0, repository.release("running", 2));
        assertEquals(1, repository.expedite("waiting"));
        assertEquals(0, repository.expedite("still-running"));

        ExportJob running = load("running");
        assertEquals(ExportStatus.QUEUED, running.getStatus());
        assertEquals(2, running.getAttempts());
        assertEquals(1, running.getReleases());
        assertNull(running.getHeartbeatAt());
        assertNull(load("waiting").getHeartbeatAt());
    }

    @Test
    @DisplayName("Finds waiting jobs: handed back first, then oldest, limited to the free threads")
    void waitingJobs() {
        save("new", ExportStatus.QUEUED, 0, NOW);
        save("old", ExportStatus.QUEUED, 0, CUTOFF.minusMinutes(5));
        save("older", ExportStatus.QUEUED, 1, CUTOFF.minusMinutes(9));
        save("handed-back", ExportStatus.QUEUED, 1, null);
        save("running", ExportStatus.RUNNING, 1, CUTOFF.minusMinutes(9));

        List<ExportJob> three = repository.findWaiting(CUTOFF, PageRequest.of(0, 3));
        List<ExportJob> one = repository.findWaiting(CUTOFF, PageRequest.of(0, 1));

        assertEquals(List.of("handed-back", "older", "old"), ids(three));
        assertEquals(List.of("handed-back"), ids(one));
    }

    @Test
    @DisplayName("Finds succeeded exports past expiry and expires each one only once")
    void expiry() {
        ExportJob expired = job("expired", ExportStatus.SUCCEEDED, 1, null);
        expired.setExpiresAt(NOW.minusMinutes(1));
        repository.save(expired);
        ExportJob fresh = job("fresh", ExportStatus.SUCCEEDED, 1, null);
        fresh.setExpiresAt(NOW.plusMinutes(1));
        repository.save(fresh);

        List<ExportJob> found = repository.findByStatusAndExpiresAtBefore(ExportStatus.SUCCEEDED, NOW,
                PageRequest.of(0, 10));

        assertEquals(List.of("expired"), ids(found));
        assertEquals(1, repository.expire("expired"));
        assertEquals(0, repository.expire("expired"));
        assertEquals(ExportStatus.EXPIRED, load("expired").getStatus());
    }

    private static void save(String jobId, ExportStatus status, int attempts, LocalDateTime heartbeatAt) {
        repository.save(job(jobId, status, attempts, heartbeatAt));
    }

    private static ExportJob job(String jobId, ExportStatus status, int attempts, LocalDateTime heartbeatAt) {
        ExportJob job = new ExportJob();
        job.setJobId(jobId);
        job.setTenant("oaf");
        job.setType(ExportType.TRANSFERS);
        job.setStatus(status);
        job.setRequestedBy("ash");
        job.setAttempts(attempts);
        job.setHeartbeatAt(heartbeatAt);
        return job;
    }

    private static ExportJob load(String jobId) {
        return repository.findByJobId(jobId).orElseThrow(IllegalStateException::new);
    }

    private static List<String> ids(List<ExportJob> jobs) {
        return jobs.stream().map(ExportJob::getJobId).collect(Collectors.toList());
    }
}
