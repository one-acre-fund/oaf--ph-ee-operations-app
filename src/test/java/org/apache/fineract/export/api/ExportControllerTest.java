package org.apache.fineract.export.api;

import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.export.ExportException;
import org.apache.fineract.export.ExportJob;
import org.apache.fineract.export.ExportJobService;
import org.apache.fineract.export.ExportStatus;
import org.apache.fineract.export.ExportType;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest {

    private final ExportJobService service = mock(ExportJobService.class);
    private final ExportController controller = new ExportController(service);
    private final AppUser user = new AppUser();

    @BeforeEach
    void setUp() {
        TenantServerConnection tenant = mock(TenantServerConnection.class);
        when(tenant.getSchemaName()).thenReturn("oaf");
        ThreadLocalContextUtil.setTenant(tenant);
        ThreadLocalContextUtil.setCurrentUser(user);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContextUtil.clear();
    }

    @Test
    @DisplayName("POST answers 202 with the job id and where to poll")
    void createReturnsAccepted() {
        ExportRequest request = new ExportRequest();
        when(service.create(request, user, "oaf")).thenReturn(job(ExportStatus.QUEUED));

        ResponseEntity<ExportJobResponse> response = controller.create(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("/api/v1/exports/job-1", response.getHeaders().getLocation().toString());
        assertEquals("job-1", response.getBody().getJobId());
        assertNull(response.getBody().getDownloadUrl());
    }

    @Test
    @DisplayName("Status shows the download link once done, and the error when failed")
    void statusAndList() {
        ExportJob failed = job(ExportStatus.FAILED);
        failed.setErrorCode("READ_FAILED");
        failed.setErrorMessage("Reading failed");
        when(service.list(user)).thenReturn(Collections.singletonList(failed));

        ExportJob succeeded = job(ExportStatus.SUCCEEDED);
        succeeded.setRowCount(12L);
        succeeded.setFileSizeBytes(345L);
        succeeded.setStartedAt(LocalDateTime.of(2026, 10, 19, 9, 31));
        succeeded.setCompletedAt(LocalDateTime.of(2026, 10, 19, 9, 32));
        succeeded.setExpiresAt(LocalDateTime.of(2026, 10, 20, 9, 32));
        succeeded.setAttempts(2);
        when(service.get("job-1", user)).thenReturn(succeeded);

        ExportJobResponse done = controller.get("job-1");
        List<ExportJobResponse> list = controller.list();

        assertEquals(ExportType.TRANSFERS, done.getType());
        assertEquals(ExportStatus.SUCCEEDED, done.getStatus());
        assertEquals(Long.valueOf(12), done.getRowCount());
        assertEquals(Long.valueOf(345), done.getFileSizeBytes());
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 30), done.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 31), done.getStartedAt());
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 32), done.getCompletedAt());
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 32), done.getExpiresAt());
        assertEquals(2, done.getAttempts());
        assertEquals("/api/v1/exports/job-1", done.getStatusUrl());
        assertNull(done.getError());
        assertEquals("/api/v1/exports/job-1/download", done.getDownloadUrl());
        assertEquals("READ_FAILED", list.get(0).getError().getCode());
        assertEquals("Reading failed", list.get(0).getError().getMessage());
    }

    @Test
    @DisplayName("Download streams the file as a CSV attachment named after the export")
    void downloadStreamsFile() throws IOException {
        ExportJob job = job(ExportStatus.SUCCEEDED);
        job.setFileSizeBytes(4L);
        when(service.prepareDownload("job-1", user)).thenReturn(job);
        doAnswer(i -> {
            ((java.io.OutputStream) i.getArgument(1)).write("a,b\n".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(service).download(eq(job), any());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.download("job-1", response);

        assertEquals("text/csv;charset=UTF-8", response.getContentType().replace(" ", ""));
        assertEquals("attachment; filename=\"transfers_2026-10-19_09-30-00.csv\"",
                response.getHeader("Content-Disposition"));
        assertEquals(4, response.getContentLength());
        assertEquals("a,b\n", response.getContentAsString());
    }

    @Test
    @DisplayName("Download without a known file size leaves the length to the server")
    void downloadWithoutSize() throws IOException {
        when(service.prepareDownload("job-1", user)).thenReturn(job(ExportStatus.SUCCEEDED));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.download("job-1", response);

        assertEquals(0, response.getContentLength());
    }

    @Test
    @DisplayName("File name falls back to the type when the creation date is missing")
    void fileNameWithoutDate() {
        ExportJob job = job(ExportStatus.SUCCEEDED);
        job.setCreatedDate(null);
        job.setType(ExportType.TRANSACTION_REQUESTS);

        assertEquals("transaction_requests.csv", ExportController.fileName(job));
    }

    @Test
    @DisplayName("Export errors become a JSON body with the status and every detail")
    void errorHandler() {
        ResponseEntity<ExportErrorResponse> response = new ExportExceptionHandler().handle(new ExportException(
                HttpStatus.BAD_REQUEST, "Invalid export request", Collections.singletonList("startFrom: bad")));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid export request", response.getBody().getMessage());
        assertEquals(Collections.singletonList("startFrom: bad"), response.getBody().getDetails());
    }

    @Test
    @DisplayName("An unknown type in the JSON body gets the export error format, not Spring's default")
    void unknownTypeInBody() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ExportExceptionHandler()).build();

        String body = mvc.perform(post(ExportController.PATH)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"UNKNOWN\",\"startFrom\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("\"status\":400"), body);
        assertTrue(body.contains("type must be one of [TRANSFERS, TRANSACTION_REQUESTS]"), body);
    }

    private static ExportJob job(ExportStatus status) {
        ExportJob job = new ExportJob();
        job.setJobId("job-1");
        job.setType(ExportType.TRANSFERS);
        job.setStatus(status);
        job.setCreatedDate(LocalDateTime.of(2026, 10, 19, 9, 30));
        return job;
    }
}
