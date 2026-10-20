package org.apache.fineract.export;

import org.apache.fineract.exception.NoAuthorizationException;
import org.apache.fineract.export.api.ExportRequest;
import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportJobServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 9, 30);

    private final ExportJobRepository repository = mock(ExportJobRepository.class);
    private final UserScopeResolver scopeResolver = mock(UserScopeResolver.class);
    private final ExportWorker worker = mock(ExportWorker.class);
    private final ExportStorage storage = mock(ExportStorage.class);
    private final AppUser user = mock(AppUser.class);
    private ExportJobService service;

    @BeforeEach
    void setUp() {
        service = new ExportJobService(repository, new ExportFilterValidator("UTC"), scopeResolver,
                new ExportCriteriaCodec(), worker, storage, new ExportProperties(),
                Clock.fixed(Instant.parse("2026-10-19T09:30:00Z"), ZoneOffset.UTC));
        when(user.getUsername()).thenReturn("ash");
        when(scopeResolver.resolve(any(), any())).thenReturn(DataScope.UNRESTRICTED);
        when(repository.save(any(ExportJob.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Creates a QUEUED job with the validated criteria and hands it to the worker")
    void createsJob() {
        ExportJob job = service.create(ExportFilterValidatorTest.request(ExportType.TRANSFERS,
                "2026-10-01 00:00:00", null, null), user, "oaf");

        assertEquals(ExportStatus.QUEUED, job.getStatus());
        assertEquals("ash", job.getRequestedBy());
        assertEquals("oaf", job.getTenant());
        assertEquals(36, job.getJobId().length());
        assertEquals("{\"startFrom\":1790812800000,\"startTo\":null,\"direction\":\"DESC\",\"filters\":{}}",
                job.getFilters());
        verify(user).validateHasExportPermission("TRANSFER");
        verify(worker).submit("oaf", job.getJobId());
    }

    @Test
    @DisplayName("Refuses users without export permission before creating anything")
    void requiresPermission() {
        doThrow(new NoAuthorizationException("no")).when(user).validateHasExportPermission("TRANSACTION_REQUEST");

        ExportRequest request = ExportFilterValidatorTest.request(ExportType.TRANSACTION_REQUESTS, null, null, null);

        assertThrows(NoAuthorizationException.class, () -> service.create(request, user, "oaf"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Refuses a third export while two are queued or running (429)")
    void limitsActiveJobs() {
        when(repository.countByRequestedByAndStatusIn("ash", ExportStatus.active())).thenReturn(2L);

        ExportRequest request = ExportFilterValidatorTest.request(ExportType.TRANSFERS, null, null, null);

        ExportException e = assertThrows(ExportException.class, () -> service.create(request, user, "oaf"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("When the export pool is full the job stays QUEUED in the database for the scheduler")
    void poolFull() {
        doThrow(new TaskRejectedException("full")).when(worker).submit(anyString(), anyString());

        ExportJob job = service.create(ExportFilterValidatorTest.request(ExportType.TRANSFERS, null, null, null),
                user, "oaf");

        assertEquals(ExportStatus.QUEUED, job.getStatus());
        assertEquals(NOW, job.getHeartbeatAt());
        verify(repository, times(1)).save(any(ExportJob.class));
    }

    @Test
    @DisplayName("A job is only visible to the user who requested it (404 otherwise)")
    void ownerOnly() {
        ExportJob job = job(ExportStatus.RUNNING, null);
        when(repository.findByJobIdAndRequestedBy("job-1", "ash")).thenReturn(Optional.of(job));

        assertSame(job, service.get("job-1", user));
        ExportException e = assertThrows(ExportException.class, () -> service.get("job-2", user));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        verify(user).validateHasExportPermission("TRANSACTION_REQUEST");
    }

    @Test
    @DisplayName("Lists the user's exports of the last 7 days")
    void listsRecentJobs() {
        List<ExportJob> jobs = Collections.singletonList(job(ExportStatus.SUCCEEDED, null));
        when(repository.findByRequestedByAndCreatedDateAfterOrderByCreatedDateDesc("ash", NOW.minusDays(7)))
                .thenReturn(jobs);

        assertEquals(jobs, service.list(user));
    }

    @Test
    @DisplayName("Lists only the types the user may still export, and 403 when they may export none")
    void listChecksExportPermission() {
        ExportJob transfers = job(ExportStatus.SUCCEEDED, null);
        transfers.setType(ExportType.TRANSFERS);
        ExportJob requests = job(ExportStatus.SUCCEEDED, null);
        when(repository.findByRequestedByAndCreatedDateAfterOrderByCreatedDateDesc("ash", NOW.minusDays(7)))
                .thenReturn(Arrays.asList(transfers, requests));
        when(user.hasNotPermissionForAnyOf("ALL_FUNCTIONS", "EXPORT_TRANSACTION_REQUEST")).thenReturn(true);

        assertEquals(Collections.singletonList(transfers), service.list(user));

        when(user.hasNotPermissionForAnyOf("ALL_FUNCTIONS", "EXPORT_TRANSFER")).thenReturn(true);
        assertThrows(NoAuthorizationException.class, () -> service.list(user));
    }

    @Test
    @DisplayName("Downloads only succeeded, unexpired jobs: 409 while not done, 410 once expired")
    void downloadRules() {
        when(repository.findByJobIdAndRequestedBy(eq("running"), anyString()))
                .thenReturn(Optional.of(job(ExportStatus.RUNNING, null)));
        when(repository.findByJobIdAndRequestedBy(eq("expired"), anyString()))
                .thenReturn(Optional.of(job(ExportStatus.SUCCEEDED, NOW)));
        ExportJob ready = job(ExportStatus.SUCCEEDED, NOW.plusHours(1));
        when(repository.findByJobIdAndRequestedBy(eq("ready"), anyString())).thenReturn(Optional.of(ready));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ExportException.class, () -> service.prepareDownload("running", user)).getStatus());
        assertEquals(HttpStatus.GONE,
                assertThrows(ExportException.class, () -> service.prepareDownload("expired", user)).getStatus());
        when(repository.findByJobIdAndRequestedBy(eq("cleaned-up"), anyString()))
                .thenReturn(Optional.of(job(ExportStatus.EXPIRED, NOW)));
        assertEquals(HttpStatus.GONE,
                assertThrows(ExportException.class, () -> service.prepareDownload("cleaned-up", user)).getStatus());
        service.download(service.prepareDownload("ready", user), out);
        when(repository.findByJobIdAndRequestedBy(eq("no-expiry"), anyString()))
                .thenReturn(Optional.of(job(ExportStatus.SUCCEEDED, null)));
        assertEquals(ExportStatus.SUCCEEDED, service.prepareDownload("no-expiry", user).getStatus());

        verify(storage).copyTo("exports/oaf/job-1.csv", out);
    }

    private static ExportJob job(ExportStatus status, LocalDateTime expiresAt) {
        ExportJob job = new ExportJob();
        job.setJobId("job-1");
        job.setType(ExportType.TRANSACTION_REQUESTS);
        job.setStatus(status);
        job.setExpiresAt(expiresAt);
        job.setStorageKey("exports/oaf/job-1.csv");
        return job;
    }
}
