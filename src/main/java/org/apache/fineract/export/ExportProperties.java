package org.apache.fineract.export;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "export")
public class ExportProperties {

    private int chunkSize = 1000;

    /** Exports running at the same time on one pod. */
    private int workerThreads = 2;

    /** Exports waiting in memory for a thread on one pod; beyond this, new exports wait in the database. */
    private int queueCapacity = 10;

    /** Exports one user may have queued or running at the same time (429 beyond). */
    private int maxActivePerUser = 2;

    private Duration ttl = Duration.ofHours(24);

    /** An export with more rows fails (EXPORT_TOO_LARGE) instead of filling the disk and the bucket. */
    private long maxRows = 5_000_000;

    /** A RUNNING job without a heartbeat for this long is taken back; a QUEUED one is picked up by any pod. */
    private Duration staleAfter = Duration.ofMinutes(2);

    /** Runs of one job before it is failed (ABANDONED), so a job that keeps killing pods does not loop forever. */
    private int maxAttempts = 3;

    private String storagePrefix = "exports/";

    /** Start files with a UTF-8 byte order mark, so Excel reads non-English characters correctly. */
    private boolean withBom = true;
}
