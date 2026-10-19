package org.apache.fineract.export;

import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "export_jobs")
public class ExportJob extends AbstractPersistableCustom<Long> {

    @Column(name = "job_id", nullable = false, unique = true, length = 36)
    private String jobId;

    @Column(name = "tenant", nullable = false)
    private String tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false)
    private ExportType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    /** Validated filters as JSON, so the job can be re-run and audited. */
    @Lob
    @Column(name = "filters")
    private String filters;

    /** The requester's data scope at creation time, as JSON. */
    @Lob
    @Column(name = "data_scope")
    private String dataScope;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
