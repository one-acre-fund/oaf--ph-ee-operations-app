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

    /** Exports waiting for a thread on one pod; beyond this a new export is refused (503). */
    private int queueCapacity = 10;

    /** Exports one user may have queued or running at the same time (429 beyond). */
    private int maxActivePerUser = 2;

    private Duration ttl = Duration.ofHours(24);

    private String storagePrefix = "exports/";

    /** Start files with a UTF-8 byte order mark, so Excel reads non-English characters correctly. */
    private boolean withBom = true;
}
