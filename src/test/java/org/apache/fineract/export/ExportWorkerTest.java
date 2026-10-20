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
import org.springframework.core.task.TaskRejectedException;
import org.apache.commons.io.FileUtils;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportWorkerTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:30:00Z");
    private static final LocalDateTime NOW_UTC = LocalDateTime.of(2026, 10, 19, 9, 30);
    private static EntityManagerFactory factory;

    private final ExportJobRepository repository = mock(ExportJobRepository.class);
    private final ExportStorage storage = mock(ExportStorage.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ExportCriteriaCodec codec = new ExportCriteriaCodec();
    private final ExportProperties properties = new ExportProperties();
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
                new ExportDateFormatter("UTC"), properties, Clock.fixed(NOW, ZoneOffset.UTC));
        when(storage.keyFor(anyString(), anyString(), anyInt())).thenAnswer(i -> "exports/" + i.getArgument(0) + "/"
                + i.getArgument(1) + "-" + i.getArgument(2) + ".csv");
        doAnswer(i -> {
            Path file = i.getArgument(1);
            uploadedFile.set(file);
            uploadedContent.set(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            return null;
        }).when(storage).upload(anyString(), any(Path.class));
        when(repository.claim(eq("job-1"), eq(1), any())).thenReturn(1);
        when(repository.heartbeat(anyString(), anyInt(), any())).thenReturn(1);
        when(repository.succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any())).thenReturn(1);
        when(repository.fail(anyString(), anyInt(), anyString(), anyString(), any())).thenReturn(1);
        when(repository.release(anyString(), anyInt())).thenReturn(1);
        when(repository.expedite(anyString())).thenReturn(1);
        doAnswer(i -> {
            ((Runnable) i.getArgument(1)).run();
            return null;
        }).when(tenantContext).runAs(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("Writes every row to a CSV, uploads it and marks the job SUCCEEDED with row count and expiry")
    void exportsTransfers() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);

        worker.run("job-1");

        verify(repository).succeed("job-1", 1, 3, uploadedContent.get().getBytes(StandardCharsets.UTF_8).length,
                "exports/oaf/job-1-1.csv", NOW_UTC, NOW_UTC.plusHours(24));
        String[] lines = uploadedContent.get().split("\r\n");
        assertTrue(lines[0].startsWith("﻿WORKFLOWINSTANCEKEY,"), lines[0]);
        assertEquals(4, lines.length);
        assertTrue(lines[1].startsWith("3,0,t3,1970-01-01T00:00:03Z,"), lines[1]);
        assertFalse(Files.exists(uploadedFile.get()), "temp file deleted");
        verify(repository, never()).heartbeat(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("Exports transaction requests with the stored data scope")
    void exportsTransactionRequests() {
        job(ExportType.TRANSACTION_REQUESTS, ExportCriteria.builder().build(),
                DataScope.builder().payeePartyIds(Collections.singletonList("duka-1")).build());

        worker.run("job-1");

        verify(repository).succeed(eq("job-1"), eq(1), eq(1L), anyLong(), anyString(), any(), any());
        assertTrue(uploadedContent.get().contains(",r1,"), uploadedContent.get());
    }

    @Test
    @DisplayName("A failed upload marks the job FAILED (UPLOAD_FAILED) and still deletes the temp file")
    void uploadFailure() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        doAnswer(i -> {
            uploadedFile.set(i.getArgument(1));
            throw new IllegalStateException("S3 down");
        }).when(storage).upload(anyString(), any(Path.class));

        worker.run("job-1");

        verify(repository).fail("job-1", 1, ExportWorker.UPLOAD_FAILED, "Storing the export file failed", NOW_UTC);
        verify(repository, never()).succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any());
        assertFalse(Files.exists(uploadedFile.get()), "temp file deleted");
    }

    @Test
    @DisplayName("A failure while reading marks the job FAILED (READ_FAILED) without uploading anything")
    void readFailure() {
        job(ExportType.TRANSFERS, ExportCriteria.builder()
                .filters(Collections.singletonMap(Filter.EXTERNALID, Collections.singletonList("x"))).build(),
                DataScope.UNRESTRICTED);

        worker.run("job-1");

        verify(repository).fail("job-1", 1, ExportWorker.READ_FAILED, "Reading the data or writing the file failed",
                NOW_UTC);
        verify(storage, never()).upload(anyString(), any(Path.class));
    }

    @Test
    @DisplayName("A temp file that cannot be created marks the job FAILED (READ_FAILED)")
    void tempFileCreationFailure() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createTempFile(anyString(), anyString(), any())).thenThrow(new IOException("full"));

            worker.run("job-1");
        }

        verify(repository).fail(eq("job-1"), eq(1), eq(ExportWorker.READ_FAILED), anyString(), any());
    }

    @Test
    @DisplayName("A temp file that cannot be deleted is only logged; the export still succeeds")
    void tempFileDeleteFailure() throws IOException {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
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

        verify(repository).succeed(eq("job-1"), eq(1), eq(3L), anyLong(), anyString(), any(), any());
        assertTrue(Files.exists(uploadedFile.get()));
        FileUtils.deleteDirectory(uploadedFile.get().toFile());
    }

    @Test
    @DisplayName("Does nothing when another worker already claimed the job")
    void alreadyClaimed() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        when(repository.claim(eq("job-1"), eq(1), any())).thenReturn(0);

        worker.run("job-1");

        verify(storage, never()).upload(anyString(), any(Path.class));
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Claims a requeued job as the next attempt and uploads under that attempt's key")
    void claimsNextAttempt() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED).setAttempts(2);
        when(repository.claim(eq("job-1"), eq(3), any())).thenReturn(1);

        worker.run("job-1");

        verify(repository).succeed(eq("job-1"), eq(3), eq(3L), anyLong(), eq("exports/oaf/job-1-3.csv"), any(),
                any());
    }

    @Test
    @DisplayName("Keeps the heartbeat going while uploading; a failed heartbeat does not stop the next ones")
    void heartbeatsDuringUpload() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setStaleAfter(Duration.ofMillis(4));
        CountDownLatch beat = new CountDownLatch(1);
        when(repository.heartbeat(anyString(), anyInt(), any())).thenThrow(new IllegalStateException("db busy"))
                .thenAnswer(i -> {
                    beat.countDown();
                    return 1;
                });
        doAnswer(i -> beat.await(5, TimeUnit.SECONDS)).when(storage).upload(anyString(), any(Path.class));

        worker.run("job-1");

        assertEquals(0, beat.getCount());
        verify(repository).succeed(eq("job-1"), eq(1), eq(3L), anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Sends a heartbeat for the run that claimed the job while reading")
    void heartbeats() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setStaleAfter(Duration.ZERO);
        properties.setChunkSize(1);

        worker.run("job-1");

        verify(repository, times(3)).heartbeat("job-1", 1, NOW_UTC);
        verify(repository).succeed(eq("job-1"), eq(1), eq(3L), anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Stops quietly, without uploading or recording anything, once the job was taken back")
    void takenBackWhileReading() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setStaleAfter(Duration.ZERO);
        when(repository.heartbeat(anyString(), anyInt(), any())).thenReturn(0);

        worker.run("job-1");

        verify(storage, never()).upload(anyString(), any(Path.class));
        verify(repository, never()).succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any());
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("A run taken back just before finishing deletes its own file and does not record a failure")
    void takenBackWhileFinishing() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        when(repository.succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any())).thenReturn(0);
        doThrow(new IllegalStateException("S3 down")).when(storage).delete(anyString());

        worker.run("job-1");

        verify(storage).delete("exports/oaf/job-1-1.csv");
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
        assertFalse(Files.exists(uploadedFile.get()), "temp file deleted");
    }

    @Test
    @DisplayName("A run taken back just before finishing leaves no file behind")
    void takenBackWhileFinishingDeletesFile() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        when(repository.succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any())).thenReturn(0);

        worker.run("job-1");

        verify(storage).delete("exports/oaf/job-1-1.csv");
    }

    @Test
    @DisplayName("An export over the row cap fails with EXPORT_TOO_LARGE before uploading")
    void tooLarge() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setMaxRows(2);
        properties.setChunkSize(1);

        worker.run("job-1");

        verify(repository).fail("job-1", 1, ExportWorker.EXPORT_TOO_LARGE,
                "The export has more than 2 rows; narrow the dates or filters", NOW_UTC);
        verify(storage, never()).upload(anyString(), any(Path.class));
    }

    @Test
    @DisplayName("A failure that cannot be recorded because the job was taken back is only logged")
    void failureAfterTakenBack() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        doAnswer(i -> {
            throw new IllegalStateException("S3 down");
        }).when(storage).upload(anyString(), any(Path.class));
        when(repository.fail(anyString(), anyInt(), anyString(), anyString(), any())).thenReturn(0);

        worker.run("job-1");

        verify(repository).fail(eq("job-1"), eq(1), eq(ExportWorker.UPLOAD_FAILED), anyString(), any());
    }

    @Test
    @DisplayName("Submitting runs the job on the executor inside the job's tenant")
    void submitRunsInTenant() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);

        worker.submit("oaf", "job-1");

        verify(tenantContext).runAs(eq("oaf"), any(Runnable.class));
        verify(storage).upload(eq("exports/oaf/job-1-1.csv"), any(Path.class));
    }

    @Test
    @DisplayName("Skips a job that no longer exists")
    void jobMissing() {
        when(repository.findByJobId("job-1")).thenReturn(Optional.empty());

        worker.run("job-1");

        verify(repository, never()).claim(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("On shutdown a running job is handed back without being recorded; the run stops at its next chunk")
    void shutdownHandsBackRunningJob() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setStaleAfter(Duration.ZERO);
        properties.setChunkSize(1);
        when(repository.heartbeat(anyString(), anyInt(), any())).thenAnswer(i -> {
            worker.destroy();
            return 1;
        });

        worker.run("job-1");

        verify(repository, times(2)).release("job-1", 1);
        verify(repository).heartbeat(anyString(), anyInt(), any());
        verify(storage, never()).upload(anyString(), any(Path.class));
        verify(repository, never()).succeed(anyString(), anyInt(), anyLong(), anyLong(), anyString(), any(), any());
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("An error caused by the shutdown is not recorded as a failure")
    void shutdownDuringUpload() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        doAnswer(i -> {
            worker.destroy();
            throw new IllegalStateException("interrupted");
        }).when(storage).upload(anyString(), any(Path.class));
        when(repository.release(anyString(), anyInt())).thenReturn(1, 0);

        worker.run("job-1");

        verify(repository, times(2)).release("job-1", 1);
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Jobs waiting in memory are expedited on shutdown, and not started afterwards")
    void shutdownExpeditesWaitingJobs() {
        ExportWorker queueing = new ExportWorker(task -> { }, tenantContext, repository, factory, storage, codec,
                new ExportDateFormatter("UTC"), properties, Clock.fixed(NOW, ZoneOffset.UTC));
        queueing.submit("oaf", "job-1");
        queueing.submit("oaf", "job-2");
        when(repository.expedite("job-2")).thenReturn(0);

        queueing.destroy();
        queueing.run("job-1");

        verify(repository).expedite("job-1");
        verify(repository, never()).claim(anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("A job the executor refuses is not remembered as waiting")
    void refusedSubmitIsForgotten() {
        ExportWorker refusing = new ExportWorker(task -> {
            throw new TaskRejectedException("full");
        }, tenantContext, repository, factory, storage, codec, new ExportDateFormatter("UTC"), properties,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(TaskRejectedException.class, () -> refusing.submit("oaf", "job-1"));
        refusing.destroy();

        verify(repository, never()).expedite(anyString());
    }

    @Test
    @DisplayName("A hand-back that fails is only logged; the heartbeat recovers the job later")
    void handBackFailure() {
        job(ExportType.TRANSFERS, ExportCriteria.builder().build(), DataScope.UNRESTRICTED);
        properties.setStaleAfter(Duration.ZERO);
        properties.setChunkSize(1);
        when(repository.heartbeat(anyString(), anyInt(), any())).thenAnswer(i -> {
            worker.destroy();
            return 1;
        });
        when(repository.release(anyString(), anyInt())).thenThrow(new IllegalStateException("db closed"));

        worker.run("job-1");

        verify(repository, times(2)).release("job-1", 1);
        verify(repository, never()).fail(anyString(), anyInt(), anyString(), anyString(), any());
    }

    private ExportJob job(ExportType type, ExportCriteria criteria, DataScope scope) {
        ExportJob job = new ExportJob();
        job.setJobId("job-1");
        job.setTenant("oaf");
        job.setType(type);
        job.setStatus(ExportStatus.QUEUED);
        job.setRequestedBy("ash");
        job.setFilters(codec.filtersToJson(criteria));
        job.setDataScope(codec.scopeToJson(scope));
        when(repository.findByJobId("job-1")).thenReturn(Optional.of(job));
        return job;
    }
}
