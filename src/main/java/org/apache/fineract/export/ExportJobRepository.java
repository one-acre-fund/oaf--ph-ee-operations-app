package org.apache.fineract.export;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * State changes are single conditional UPDATEs, so they stay correct with several pods and several runs of a job.
 * A run identifies itself by the attempt number it claimed; once the job is taken back, that run can no longer
 * change it.
 */
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    String STATUS = "org.apache.fineract.export.ExportStatus.";

    Optional<ExportJob> findByJobId(String jobId);

    Optional<ExportJob> findByJobIdAndRequestedBy(String jobId, String requestedBy);

    List<ExportJob> findByRequestedByAndCreatedDateAfterOrderByCreatedDateDesc(String requestedBy, LocalDateTime after);

    long countByRequestedByAndStatusIn(String requestedBy, Collection<ExportStatus> statuses);

    /**
     * QUEUED jobs that no pod is about to run: handed back on shutdown (no heartbeat) or untouched since
     * {@code cutoff}. Handed-back jobs come first, then the oldest.
     */
    @Query("SELECT j FROM ExportJob j WHERE j.status = " + STATUS + "QUEUED "
            + "AND (j.heartbeatAt IS NULL OR j.heartbeatAt < :cutoff) ORDER BY j.heartbeatAt")
    List<ExportJob> findWaiting(@Param("cutoff") LocalDateTime cutoff, Pageable page);

    List<ExportJob> findByStatusAndExpiresAtBefore(ExportStatus status, LocalDateTime before, Pageable page);

    /**
     * Moves a job from QUEUED to RUNNING as run {@code attempt}, the attempts the caller read plus one, so only one
     * run (on any pod) gets it and each run knows its own number.
     * @return 1 if this caller claimed the job, 0 if it was already claimed or is no longer queued
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "RUNNING, j.startedAt = :now, j.heartbeatAt = :now, "
            + "j.attempts = :attempt WHERE j.jobId = :jobId AND j.attempts = :attempt - 1 AND j.status = " + STATUS
            + "QUEUED")
    int claim(@Param("jobId") String jobId, @Param("attempt") int attempt, @Param("now") LocalDateTime now);

    /** @return 0 if the job was taken back from this run */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.heartbeatAt = :now WHERE j.jobId = :jobId AND j.attempts = :attempt "
            + "AND j.status = " + STATUS + "RUNNING")
    int heartbeat(@Param("jobId") String jobId, @Param("attempt") int attempt, @Param("now") LocalDateTime now);

    /** @return 0 if the job was taken back from this run */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "SUCCEEDED, j.rowCount = :rows, j.fileSizeBytes = :size, "
            + "j.storageKey = :key, j.completedAt = :now, j.expiresAt = :expiresAt "
            + "WHERE j.jobId = :jobId AND j.attempts = :attempt AND j.status = " + STATUS + "RUNNING")
    int succeed(@Param("jobId") String jobId, @Param("attempt") int attempt, @Param("rows") long rows,
                @Param("size") long size, @Param("key") String key, @Param("now") LocalDateTime now,
                @Param("expiresAt") LocalDateTime expiresAt);

    /** @return 0 if the job was taken back from this run */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "FAILED, j.errorCode = :code, j.errorMessage = :message, "
            + "j.completedAt = :now WHERE j.jobId = :jobId AND j.attempts = :attempt AND j.status = " + STATUS
            + "RUNNING")
    int fail(@Param("jobId") String jobId, @Param("attempt") int attempt, @Param("code") String code,
             @Param("message") String message, @Param("now") LocalDateTime now);

    /**
     * Puts RUNNING jobs whose heartbeat stopped before {@code cutoff} back in the queue, if attempts are left. The
     * stale heartbeat is kept, so {@link #findWaiting} picks them up at once.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "QUEUED WHERE j.status = " + STATUS + "RUNNING "
            + "AND j.heartbeatAt < :cutoff AND j.attempts - j.releases < :maxAttempts")
    int requeueStale(@Param("cutoff") LocalDateTime cutoff, @Param("maxAttempts") int maxAttempts);

    /** Fails RUNNING jobs whose heartbeat stopped before {@code cutoff} and that have no attempts left. */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "FAILED, j.errorCode = :code, j.errorMessage = :message, "
            + "j.completedAt = :now WHERE j.status = " + STATUS + "RUNNING AND j.heartbeatAt < :cutoff "
            + "AND j.attempts - j.releases >= :maxAttempts")
    int abandonStale(@Param("cutoff") LocalDateTime cutoff, @Param("maxAttempts") int maxAttempts,
                     @Param("code") String code, @Param("message") String message, @Param("now") LocalDateTime now);

    /**
     * Hands a running job back to the queue when its pod shuts down, without counting the attempt. Clearing the
     * heartbeat makes it the first job another pod picks up.
     * @return 0 if the job was finished or taken back meanwhile
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "QUEUED, j.releases = j.releases + 1, j.heartbeatAt = NULL "
            + "WHERE j.jobId = :jobId AND j.attempts = :attempt AND j.status = " + STATUS + "RUNNING")
    int release(@Param("jobId") String jobId, @Param("attempt") int attempt);

    /** Lets another pod pick up a job that was waiting in this pod's memory when it shut down. */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.heartbeatAt = NULL WHERE j.jobId = :jobId AND j.status = " + STATUS + "QUEUED")
    int expedite(@Param("jobId") String jobId);

    /** @return 0 if the job is no longer SUCCEEDED, e.g. another pod expired it first */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = " + STATUS + "EXPIRED WHERE j.jobId = :jobId AND j.status = "
            + STATUS + "SUCCEEDED")
    int expire(@Param("jobId") String jobId);
}
