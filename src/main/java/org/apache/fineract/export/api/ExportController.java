package org.apache.fineract.export.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.export.ExportJob;
import org.apache.fineract.export.ExportJobService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "BearerAuth")
@RequestMapping(ExportController.PATH)
public class ExportController {

    public static final String PATH = "/api/v1/exports";

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ExportJobService service;

    public ExportController(ExportJobService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExportJobResponse> create(@RequestBody ExportRequest request) {
        ExportJob job = service.create(request, ThreadLocalContextUtil.getCurrentUser(),
                ThreadLocalContextUtil.getTenant().getSchemaName());
        ExportJobResponse response = new ExportJobResponse(job);
        return ResponseEntity.accepted().location(URI.create(response.getStatusUrl())).body(response);
    }

    @GetMapping("/{jobId}")
    public ExportJobResponse get(@PathVariable String jobId) {
        return new ExportJobResponse(service.get(jobId, ThreadLocalContextUtil.getCurrentUser()));
    }

    @GetMapping
    public List<ExportJobResponse> list() {
        return service.list(ThreadLocalContextUtil.getCurrentUser()).stream()
                .map(ExportJobResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Streams the file from storage straight to the response; it is never held in memory.
     */
    @GetMapping("/{jobId}/download")
    public void download(@PathVariable String jobId, HttpServletResponse response) throws IOException {
        ExportJob job = service.prepareDownload(jobId, ThreadLocalContextUtil.getCurrentUser());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName(job) + "\"");
        if (job.getFileSizeBytes() != null) {
            response.setContentLengthLong(job.getFileSizeBytes());
        }
        service.download(job, response.getOutputStream());
    }

    static String fileName(ExportJob job) {
        String prefix = job.getType().name().toLowerCase();
        return job.getCreatedDate() == null ? prefix + ".csv" : prefix + "_" + FILE_DATE.format(job.getCreatedDate()) + ".csv";
    }
}
