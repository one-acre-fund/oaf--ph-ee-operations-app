package org.apache.fineract.export;

import org.apache.fineract.export.api.ExportRequest;
import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.organisation.user.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Creates, looks up and serves export jobs. A job is only ever visible to the user who requested it.
 */
@Service
@ConditionalOnProperty(name = "export.async.enabled", havingValue = "true")
public class ExportJobService {

    static final String QUEUE_FULL = "QUEUE_FULL";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExportJobRepository jobRepository;
    private final ExportFilterValidator validator;
    private final UserScopeResolver scopeResolver;
    private final ExportCriteriaCodec codec;
    private final ExportWorker worker;
    private final ExportStorage storage;
    private final ExportProperties properties;
    private final Clock clock;

    @SuppressWarnings("java:S107") // collaborators of the service, injected by Spring
    public ExportJobService(ExportJobRepository jobRepository, ExportFilterValidator validator,
                            UserScopeResolver scopeResolver, ExportCriteriaCodec codec, ExportWorker worker,
                            ExportStorage storage, ExportProperties properties,
                            @Qualifier("exportClock") Clock clock) {
        this.jobRepository = jobRepository;
        this.validator = validator;
        this.scopeResolver = scopeResolver;
        this.codec = codec;
        this.worker = worker;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    public ExportJob create(ExportRequest request, AppUser user, String tenant) {
        ExportCriteria criteria = validator.validate(request);
        ExportType type = request.getType();
        user.validateHasExportPermission(resourceName(type));
        DataScope scope = scopeResolver.resolve(user, type);
        if (jobRepository.countByRequestedByAndStatusIn(user.getUsername(), ExportStatus.active())
                >= properties.getMaxActivePerUser()) {
            throw new ExportException(HttpStatus.TOO_MANY_REQUESTS, "You already have "
                    + properties.getMaxActivePerUser() + " exports queued or running; wait for one to finish");
        }

        ExportJob job = new ExportJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setTenant(tenant);
        job.setType(type);
        job.setStatus(ExportStatus.QUEUED);
        job.setRequestedBy(user.getUsername());
        job.setFilters(codec.filtersToJson(criteria));
        job.setDataScope(codec.scopeToJson(scope));
        job = jobRepository.save(job);

        try {
            worker.submit(tenant, job.getJobId());
        } catch (TaskRejectedException e) {
            logger.warn("Export {} refused: export queue is full", job.getJobId());
            job.setStatus(ExportStatus.FAILED);
            job.setErrorCode(QUEUE_FULL);
            job.setErrorMessage("Too many exports are running; try again in a few minutes");
            job.setCompletedAt(LocalDateTime.now(clock));
            jobRepository.save(job);
            throw new ExportException(HttpStatus.SERVICE_UNAVAILABLE, job.getErrorMessage());
        }
        return job;
    }

    public ExportJob get(String jobId, AppUser user) {
        ExportJob job = jobRepository.findByJobIdAndRequestedBy(jobId, user.getUsername())
                .orElseThrow(() -> ExportException.notFound(jobId));
        user.validateHasExportPermission(resourceName(job.getType()));
        return job;
    }

    /** The user's exports from the last 7 days, newest first. */
    public List<ExportJob> list(AppUser user) {
        return jobRepository.findByRequestedByAndCreatedDateAfterOrderByCreatedDateDesc(user.getUsername(),
                LocalDateTime.now(clock).minusDays(7));
    }

    /**
     * Checks that the job can be downloaded now; call before writing any response headers.
     */
    public ExportJob prepareDownload(String jobId, AppUser user) {
        ExportJob job = get(jobId, user);
        if (job.getStatus() != ExportStatus.SUCCEEDED) {
            throw new ExportException(HttpStatus.CONFLICT, "Export " + jobId + " is " + job.getStatus());
        }
        if (job.getExpiresAt() != null && !job.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new ExportException(HttpStatus.GONE, "Export " + jobId + " has expired; run it again");
        }
        return job;
    }

    public void download(ExportJob job, OutputStream out) {
        storage.copyTo(job.getStorageKey(), out);
    }

    static String resourceName(ExportType type) {
        return type == ExportType.TRANSFERS ? "TRANSFER" : "TRANSACTION_REQUEST";
    }
}
