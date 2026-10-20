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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Runs export jobs on the bounded export pool: claim, read in chunks into a local CSV, upload, record the result.
 * Failures are logged with the job id and stored on the job; the local file is always deleted.
 */
@Component
@ConditionalOnProperty(name = "export.async.enabled", havingValue = "true")
public class ExportWorker {

    static final String READ_FAILED = "READ_FAILED";
    static final String UPLOAD_FAILED = "UPLOAD_FAILED";

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
        executor.execute(() -> tenantContext.runAs(tenant, () -> run(jobId)));
    }

    void run(String jobId) {
        if (jobRepository.claim(jobId, LocalDateTime.now(clock)) == 0) {
            logger.info("Export {} was already claimed or is no longer queued", jobId);
            return;
        }
        ExportJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("Claimed export " + jobId + " not found"));
        Path file = null;
        String failure = READ_FAILED;
        try {
            file = Files.createTempFile("export_", ".csv", OWNER_ONLY);
            long rows = writeCsv(job, codec.fromJson(job.getFilters(), job.getDataScope()), file);
            failure = UPLOAD_FAILED;
            String key = storage.keyFor(job.getTenant(), jobId);
            storage.upload(key, file);
            succeed(job, rows, Files.size(file), key);
            logger.info("Export {} finished with {} rows", jobId, rows);
        } catch (Exception e) {
            logger.error("Export {} failed ({})", jobId, failure, e);
            fail(job, failure);
        } finally {
            deleteQuietly(file);
        }
    }

    private long writeCsv(ExportJob job, ExportCriteria criteria, Path file) throws IOException {
        int chunkSize = properties.getChunkSize();
        if (job.getType() == ExportType.TRANSFERS) {
            return writeCsv(new TransferReader(entityManagerFactory, criteria, chunkSize), ExportColumns.TRANSFERS,
                    file);
        }
        return writeCsv(new TransactionRequestReader(entityManagerFactory, criteria, chunkSize),
                ExportColumns.TRANSACTION_REQUESTS, file);
    }

    private <T> long writeCsv(KeysetReader<T> reader, List<ExportColumn<T>> columns, Path file) throws IOException {
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file));
             CsvExportWriter<T> writer = new CsvExportWriter<>(out, columns, dateFormatter, properties.isWithBom())) {
            return reader.read(writer::writeAll);
        }
    }

    private void succeed(ExportJob job, long rows, long size, String key) {
        LocalDateTime now = LocalDateTime.now(clock);
        job.setStatus(ExportStatus.SUCCEEDED);
        job.setRowCount(rows);
        job.setFileSizeBytes(size);
        job.setStorageKey(key);
        job.setCompletedAt(now);
        job.setExpiresAt(now.plus(properties.getTtl()));
        jobRepository.save(job);
    }

    private void fail(ExportJob job, String code) {
        job.setStatus(ExportStatus.FAILED);
        job.setErrorCode(code);
        job.setErrorMessage(READ_FAILED.equals(code) ? "Reading the data or writing the file failed"
                : "Storing the export file failed");
        job.setCompletedAt(LocalDateTime.now(clock));
        jobRepository.save(job);
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
}
