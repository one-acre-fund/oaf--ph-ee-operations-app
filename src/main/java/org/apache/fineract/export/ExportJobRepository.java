package org.apache.fineract.export;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    Optional<ExportJob> findByJobIdAndRequestedBy(String jobId, String requestedBy);

    long countByRequestedByAndStatusIn(String requestedBy, Collection<ExportStatus> statuses);

    /**
     * Moves a job from QUEUED to RUNNING in one statement, so only one worker (on any pod) can claim it.
     * @return 1 if this caller claimed the job, 0 if it was already claimed or is no longer queued
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExportJob j SET j.status = org.apache.fineract.export.ExportStatus.RUNNING, j.startedAt = :now "
            + "WHERE j.jobId = :jobId AND j.status = org.apache.fineract.export.ExportStatus.QUEUED")
    int claim(@Param("jobId") String jobId, @Param("now") LocalDateTime now);
}
