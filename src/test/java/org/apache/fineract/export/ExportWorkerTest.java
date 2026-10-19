package org.apache.fineract.export;

import org.apache.fineract.export.csv.ExportDateFormatter;
import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.export.read.H2Entities;
import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.TransactionRequestState;
import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.TransferStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.apache.commons.io.FileUtils;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportWorkerTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:30:00Z");
    private static EntityManagerFactory factory;

    private final ExportJobRepository repository = mock(ExportJobRepository.class);
    private final ExportStorage storage = mock(ExportStorage.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ExportCriteriaCodec codec = new ExportCriteriaCodec();
    private ExportWorker worker;
    private final AtomicReference<Path> uploadedFile = new AtomicReference<>();
    private final AtomicReference<String> uploadedContent = new AtomicReference<>();

    @BeforeAll
    static void startDatabase() {
        factory = H2Entities.create("export_worker");
        H2Entities.inTransaction(factory, em -> {
            for (int i = 1; i <= 3; i++) {
                Transfer transfer = new Transfer();
                transfer.setTransactionId("t" + i);
                transfer.setWorkflowInstanceKey((long) i);
                transfer.setZeebeGeneration(0L);
                transfer.setStartedAt(new Date(i * 1000L));
                transfer.setStatus(TransferStatus.COMPLETED);
                transfer.setAmount(new BigDecimal("10.00"));
                transfer.setCurrency("KES");
                em.persist(transfer);
                em.flush();
            }
            TransactionRequest request = new TransactionRequest();
            request.setTransactionId("r1");
            request.setWorkflowInstanceKey("9");
            request.setZeebeGeneration(0L);
            request.setStartedAt(new Date(5000));
            request.setState(TransactionRequestState.ACCEPTED);
            request.setCurrency("KES");
            request.setPayeePartyId("duka-1");
            em.persist(request);
        });
    }

    @AfterAll
    static void stopDatabase() {
        factory.close();
    }

    @BeforeEach
    void setUp() {
        worker = new ExportWorker(Runnable::run, tenantContext, repository, factory, storage, codec,
                new ExportDateFormatter("UTC"), new ExportProperties(), Clock.fixed(NOW, ZoneOffset.UTC));
        when(storage.keyFor(anyString(), anyString())).thenAnswer(i -> "exports/" + i.getArgument(0) + "/"
                + i.getArgument(1) + ".csv");
        doAnswer(i -> {
            Path file = i.getArgument(1);
            uploadedFile.set(file);
            uploadedContent.set(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            return null;
        }).when(storage).upload(anyString(), any(Path.class));
        when(repository.claim(eq("job-1"), any())).thenReturn(1);
    }

    @Test
    @DisplayName("Writes every row to a CSV, uploads it and marks the job SUCCEEDED with row count and expiry")
    void exportsTransfers() {
        ExportJob job = job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);

        worker.run("job-1");

        assertEquals(ExportStatus.SUCCEEDED, job.getStatus());
        assertEquals(Long.valueOf(3), job.getRowCount());
        assertEquals("exports/oaf/job-1.csv", job.getStorageKey());
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 30), job.getCompletedAt());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 30), job.getExpiresAt());
        assertEquals(Long.valueOf(uploadedContent.get().getBytes(StandardCharsets.UTF_8).length), job.getFileSizeBytes());
        String[] lines = uploadedContent.get().split("\r\n");
        assertTrue(lines[0].startsWith("﻿WORKFLOWINSTANCEKEY,"), lines[0]);
        assertEquals(4, lines.length);
        assertTrue(lines[1].startsWith("3,0,t3,1970-01-01T00:00:03Z,"), lines[1]);
        assertFalse(Files.exists(uploadedFile.get()), "temp file deleted");
        verify(repository).save(job);
    }

    @Test
    @DisplayName("Exports transaction requests with the stored data scope")
    void exportsTransactionRequests() {
        ExportJob job = job(ExportType.TRANSACTION_REQUESTS, ExportCriteria.builder().build(),
                DataScope.builder().payeePartyIds(Collections.singletonList("duka-1")).build());

        worker.run("job-1");

        assertEquals(ExportStatus.SUCCEEDED, job.getStatus());
        assertEquals(Long.valueOf(1), job.getRowCount());
        assertTrue(uploadedContent.get().contains(",r1,"), uploadedContent.get());
    }

    @Test
    @DisplayName("A failed upload marks the job FAILED (UPLOAD_FAILED) and still deletes the temp file")
    void uploadFailure() {
        ExportJob job = job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        doAnswer(i -> {
            uploadedFile.set(i.getArgument(1));
            throw new IllegalStateException("S3 down");
        }).when(storage).upload(anyString(), any(Path.class));

        worker.run("job-1");

        assertEquals(ExportStatus.FAILED, job.getStatus());
        assertEquals(ExportWorker.UPLOAD_FAILED, job.getErrorCode());
        assertEquals("Storing the export file failed", job.getErrorMessage());
        assertNull(job.getStorageKey());
        assertFalse(Files.exists(uploadedFile.get()), "temp file deleted");
    }

    @Test
    @DisplayName("A failure while reading marks the job FAILED (READ_FAILED) without uploading anything")
    void readFailure() {
        ExportJob job = job(ExportType.TRANSFERS, ExportCriteria.builder()
                .filters(Collections.singletonMap(Filter.EXTERNALID, Collections.singletonList("x"))).build(),
                DataScope.UNRESTRICTED);

        worker.run("job-1");

        assertEquals(ExportStatus.FAILED, job.getStatus());
        assertEquals(ExportWorker.READ_FAILED, job.getErrorCode());
        assertEquals("Reading the data or writing the file failed", job.getErrorMessage());
        verify(storage, never()).upload(anyString(), any(Path.class));
    }

    @Test
    @DisplayName("A temp file that cannot be created marks the job FAILED (READ_FAILED)")
    void tempFileCreationFailure() {
        ExportJob job = job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createTempFile(anyString(), anyString(), any())).thenThrow(new IOException("full"));

            worker.run("job-1");
        }

        assertEquals(ExportStatus.FAILED, job.getStatus());
        assertEquals(ExportWorker.READ_FAILED, job.getErrorCode());
    }

    @Test
    @DisplayName("A temp file that cannot be deleted is only logged; the export still succeeds")
    void tempFileDeleteFailure() throws IOException {
        ExportJob job = job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        // replace the temp file with a non-empty directory, which deleteIfExists cannot remove
        doAnswer(i -> {
            Path file = i.getArgument(1);
            uploadedFile.set(file);
            Files.delete(file);
            Files.createDirectory(file);
            Files.createFile(file.resolve("blocker"));
            return null;
        }).when(storage).upload(anyString(), any(Path.class));

        worker.run("job-1");

        assertEquals(ExportStatus.SUCCEEDED, job.getStatus());
        assertTrue(Files.exists(uploadedFile.get()));
        FileUtils.deleteDirectory(uploadedFile.get().toFile());
    }

    @Test
    @DisplayName("Does nothing when another worker already claimed the job")
    void alreadyClaimed() {
        when(repository.claim(eq("job-1"), any())).thenReturn(0);

        worker.run("job-1");

        verify(repository, never()).findByJobId(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Submitting runs the job on the executor inside the job's tenant")
    void submitRunsInTenant() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        doAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        }).when(tenantContext).runAs(eq("oaf"), any(Runnable.class));

        worker.submit("oaf", "job-1");

        verify(tenantContext).runAs(eq("oaf"), any(Runnable.class));
        verify(storage).upload(eq("exports/oaf/job-1.csv"), any(Path.class));
    }

    @Test
    @DisplayName("Fails loudly if a claimed job cannot be loaded")
    void claimedJobMissing() {
        when(repository.findByJobId("job-1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> worker.run("job-1"));
    }

    private ExportJob job(ExportType type, ExportCriteria criteria, DataScope scope) {
        ExportJob job = new ExportJob();
        job.setJobId("job-1");
        job.setTenant("oaf");
        job.setType(type);
        job.setStatus(ExportStatus.RUNNING);
        job.setRequestedBy("ash");
        job.setFilters(codec.filtersToJson(criteria));
        job.setDataScope(codec.scopeToJson(scope));
        when(repository.findByJobId("job-1")).thenReturn(Optional.of(job));
        return job;
    }
}
