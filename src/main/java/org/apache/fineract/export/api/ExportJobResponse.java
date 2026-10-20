package org.apache.fineract.export.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.apache.fineract.export.ExportJob;
import org.apache.fineract.export.ExportStatus;
import org.apache.fineract.export.ExportType;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportJobResponse {

    private final String jobId;
    private final ExportType type;
    private final ExportStatus status;
    private final Long rowCount;
    private final Long fileSizeBytes;
    private final LocalDateTime createdAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime expiresAt;
    /** Times the export was started; above 1 means it was restarted after an interruption. */
    private final int attempts;
    private final String statusUrl;
    private final String downloadUrl;
    private final Error error;

    public ExportJobResponse(ExportJob job) {
        this.jobId = job.getJobId();
        this.type = job.getType();
        this.status = job.getStatus();
        this.rowCount = job.getRowCount();
        this.fileSizeBytes = job.getFileSizeBytes();
        this.createdAt = job.getCreatedDate();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.expiresAt = job.getExpiresAt();
        this.attempts = job.getAttempts();
        this.statusUrl = ExportController.PATH + "/" + job.getJobId();
        this.downloadUrl = job.getStatus() == ExportStatus.SUCCEEDED ? statusUrl + "/download" : null;
        this.error = job.getErrorCode() == null ? null : new Error(job.getErrorCode(), job.getErrorMessage());
    }

    @Getter
    public static class Error {
        private final String code;
        private final String message;

        Error(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
