package org.apache.fineract.export;

import org.apache.fineract.export.csv.CsvExportWriter;
import org.apache.fineract.export.csv.ExportColumn;
import org.apache.fineract.export.csv.ExportColumns;
import org.apache.fineract.export.csv.ExportDateFormatter;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.export.read.KeysetReader;
import org.apache.fineract.export.read.TransactionRequestReader;
import org.apache.fineract.export.read.TransferReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

/**
 * Runs export jobs on the bounded export pool: claim, read in chunks into a local CSV, upload, record the result.
 * Failures are logged with the job id and stored on the job; the local file is always deleted. While reading, the
 * run sends a heartbeat after each chunk, and during the upload from a separate thread; if the job was taken back
 * meanwhile (see {@link ExportScheduler}), the run stops quietly.
 * When the pod shuts down, running and waiting jobs are handed back to the database at once, so another pod
 * continues them within one scheduler run instead of waiting for the heartbeat to go stale.
 */
@Component
public class ExportWorker implements DisposableBean {

    static final String READ_FAILED = "READ_FAILED";
    static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    static final String EXPORT_TOO_LARGE = "EXPORT_TOO_LARGE";

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskExecutor executor;
    private final TenantContext tenantContext;
    private final ExportJobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final ExportStorage storage;
    private final ExportCriteriaCodec codec;
    private final ExportDateFormatter dateFormatter;
    private final ExportProperties properties;
    private final Clock clock;

    /** Jobs this pod is running, by job id. */
    private final Map<String, Run> running = new ConcurrentHashMap<>();

    /** Jobs waiting in this pod's executor queue, by job id. */
    private final Map<String, String> waiting = new ConcurrentHashMap<>();

    private final ScheduledExecutorService uploadHeartbeats = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "export-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean stopping;

    @SuppressWarnings("java:S107") // collaborators of the worker, injected by Spring
    public ExportWorker(@Qualifier("exportExecutor") TaskExecutor executor, TenantContext tenantContext,
                        ExportJobRepository jobRepository, EntityManagerFactory entityManagerFactory,
                        ExportStorage storage, ExportCriteriaCodec codec, ExportDateFormatter dateFormatter,
                        ExportProperties properties, @Qualifier("exportClock") Clock clock) {
        this.executor = executor;
        this.tenantContext = tenantContext;
        this.jobRepository = jobRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.storage = storage;
        this.codec = codec;
        this.dateFormatter = dateFormatter;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @throws org.springframework.core.task.TaskRejectedException when the pool and its queue are full
     */
    public void submit(String tenant, String jobId) {
        waiting.put(jobId, tenant);
        try {
            executor.execute(() -> tenantContext.runAs(tenant, () -> run(jobId)));
        } catch (RuntimeException e) {
            waiting.remove(jobId);
            throw e;
        }
    }

    /**
     * Hands this pod's jobs back to the database before the connections close (this bean depends on them, so Spring
     * destroys it first). A run that is still reading notices at its next chunk and stops without writing anything.
     */
    @Override
    public void destroy() {
        stopping = true;
        running.forEach((jobId, run) -> handBack(jobId, run.tenant,
                () -> jobRepository.release(jobId, run.attempt)));
        waiting.forEach((jobId, tenant) -> handBack(jobId, tenant, () -> jobRepository.expedite(jobId)));
        uploadHeartbeats.shutdownNow();
    }

    private void handBack(String jobId, String tenant, IntSupplier update) {
        try {
            tenantContext.runAs(tenant, () -> {
                if (update.getAsInt() == 1) {
                    logger.info("Export {} handed back to the queue: this pod is shutting down", jobId);
                }
            });
        } catch (RuntimeException e) {
            logger.warn("Export {} could not be handed back; it is recovered once its heartbeat is stale", jobId, e);
        }
    }

    void run(String jobId) {
        waiting.remove(jobId);
        if (stopping) {
            logger.info("Export {} left in the queue: this pod is shutting down", jobId);
            return;
        }
        ExportJob job = jobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            logger.warn("Export {} not found", jobId);
            return;
        }
        int attempt = job.getAttempts() + 1;
        LocalDateTime claimedAt = LocalDateTime.now(clock);
        if (jobRepository.claim(jobId, attempt, claimedAt) == 0) {
            logger.info("Export {} was already claimed or is no longer queued", jobId);
            return;
        }
        Run run = new Run(job.getTenant(), attempt);
        running.put(jobId, run);
        Path file = null;
        String failure = READ_FAILED;
        try {
            file = Files.createTempFile("export_", ".csv", OWNER_ONLY);
            Progress progress = new Progress(jobId, attempt, claimedAt);
            long rows = writeCsv(job, codec.fromJson(job.getFilters(), job.getDataScope()), file, progress);
            failure = UPLOAD_FAILED;
            String key = storage.keyFor(job.getTenant(), jobId, attempt);
            upload(job.getTenant(), jobId, attempt, key, file);
            LocalDateTime now = LocalDateTime.now(clock);
            if (jobRepository.succeed(jobId, attempt, rows, Files.size(file), key, now,
                    now.plus(properties.getTtl())) == 0) {
                deleteUpload(jobId, key);
                throw new TakenBackException();
            }
            logger.info("Export {} finished with {} rows", jobId, rows);
        } catch (TakenBackException e) {
            logger.warn("Export {} run {} stopped: the job was taken back after its heartbeat stopped", jobId,
                    attempt);
        } catch (HandedBackException e) {
            stoppedForShutdown(jobId, attempt);
        } catch (TooLargeException e) {
            logger.warn("Export {} stopped: more than {} rows", jobId, properties.getMaxRows());
            fail(jobId, attempt, EXPORT_TOO_LARGE, "The export has more than " + properties.getMaxRows()
                    + " rows; narrow the dates or filters");
        } catch (Exception e) {
            if (stopping) {
                stoppedForShutdown(jobId, attempt);
            } else {
                logger.error("Export {} failed ({})", jobId, failure, e);
                fail(jobId, attempt, failure, READ_FAILED.equals(failure)
                        ? "Reading the data or writing the file failed" : "Storing the export file failed");
            }
        } finally {
            // a replacement run of the same job may have started on this pod meanwhile
            running.remove(jobId, run);
            deleteQuietly(file);
        }
    }

    private long writeCsv(ExportJob job, ExportCriteria criteria, Path file, Progress progress) throws IOException {
        int chunkSize = properties.getChunkSize();
        if (job.getType() == ExportType.TRANSFERS) {
            return writeCsv(new TransferReader(entityManagerFactory, criteria, chunkSize), ExportColumns.TRANSFERS,
                    file, progress);
        }
        return writeCsv(new TransactionRequestReader(entityManagerFactory, criteria, chunkSize),
                ExportColumns.TRANSACTION_REQUESTS, file, progress);
    }

    private <T> long writeCsv(KeysetReader<T> reader, List<ExportColumn<T>> columns, Path file, Progress progress)
            throws IOException {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file));
             CsvExportWriter<T> writer = new CsvExportWriter<>(out, columns, dateFormatter, properties.isWithBom())) {
            return reader.read(chunk -> {
                writer.writeAll(chunk);
                progress.afterChunk(chunk.size());
            });
        }
    }

    /** Keeps the heartbeat going while the file uploads; a large file can take longer than {@code staleAfter}. */
    private void upload(String tenant, String jobId, int attempt, String key, Path file) {
        long every = Math.max(1, properties.getStaleAfter().dividedBy(4).toMillis());
        ScheduledFuture<?> heartbeat = uploadHeartbeats.scheduleAtFixedRate(
                () -> uploadHeartbeat(tenant, jobId, attempt), every, every, TimeUnit.MILLISECONDS);
        try {
            storage.upload(key, file);
        } finally {
            heartbeat.cancel(false);
        }
    }

    /** Never throws: an exception would silently cancel the remaining heartbeats. */
    private void uploadHeartbeat(String tenant, String jobId, int attempt) {
        try {
            tenantContext.runAs(tenant, () -> jobRepository.heartbeat(jobId, attempt, LocalDateTime.now(clock)));
        } catch (RuntimeException e) {
            logger.warn("Export {} run {}: heartbeat during upload failed", jobId, attempt, e);
        }
    }

    private void deleteUpload(String jobId, String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            logger.warn("Export {}: could not delete the file of a run that was taken back", jobId, e);
        }
    }

    /**
     * Releases the job again in case this run claimed it after {@link #destroy()} went through the running jobs;
     * otherwise the release is a no-op.
     */
    private void stoppedForShutdown(String jobId, int attempt) {
        try {
            jobRepository.release(jobId, attempt);
            logger.info("Export {} run {} stopped: the job was handed back for another pod", jobId, attempt);
        } catch (RuntimeException e) {
            logger.warn("Export {} could not be handed back; it is recovered once its heartbeat is stale", jobId, e);
        }
    }

    private void fail(String jobId, int attempt, String code, String message) {
        if (jobRepository.fail(jobId, attempt, code, message, LocalDateTime.now(clock)) == 0) {
            logger.warn("Export {} run {} could not record its failure: the job was taken back", jobId, attempt);
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.warn("Could not delete temp file", e);
        }
    }

    /**
     * Counts rows against the cap and sends a heartbeat at most every quarter of {@code staleAfter}. A run is only
     * taken for dead when no chunk finished for {@code staleAfter}.
     */
    private final class Progress {

        private final String jobId;
        private final int attempt;
        private final Duration beatEvery;
        private LocalDateTime lastBeat;
        private long rows;

        Progress(String jobId, int attempt, LocalDateTime claimedAt) {
            this.jobId = jobId;
            this.attempt = attempt;
            this.beatEvery = properties.getStaleAfter().dividedBy(4);
            this.lastBeat = claimedAt;
        }

        void afterChunk(int size) {
            if (stopping) {
                throw new HandedBackException();
            }
            rows += size;
            if (rows > properties.getMaxRows()) {
                throw new TooLargeException();
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (!now.isBefore(lastBeat.plus(beatEvery))) {
                if (jobRepository.heartbeat(jobId, attempt, now) == 0) {
                    throw new TakenBackException();
                }
                lastBeat = now;
            }
        }
    }

    private static final class Run {

        private final String tenant;
        private final int attempt;

        Run(String tenant, int attempt) {
            this.tenant = tenant;
            this.attempt = attempt;
        }
    }

    private static final class TakenBackException extends RuntimeException {
    }

    private static final class HandedBackException extends RuntimeException {
    }

    private static final class TooLargeException extends RuntimeException {
    }
}
