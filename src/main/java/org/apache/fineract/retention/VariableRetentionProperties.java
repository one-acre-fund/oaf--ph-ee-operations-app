package org.apache.fineract.retention;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FD-1737: monthly retention of the partitioned variables table. Read on every run, so a change takes effect at
 * the next run without a release.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "variables.retention")
public class VariableRetentionProperties {

    /** The job does nothing unless switched on; the table must be partitioned by month first (V56 and the cutover). */
    private boolean enabled = false;

    /** Months kept online. A month is archived and dropped once it is entirely older than this. */
    private int months = 12;

    /** Empty monthly partitions created ahead of time, so new rows never land in pmax. */
    private int aheadMonths = 3;

    /** Prefix of the archive objects in the export bucket: {prefix}{tenant}/variables-{partition}.csv.gz */
    private String storagePrefix = "variables-archive/";

    /** Log what would be added, archived and dropped without changing anything. */
    private boolean dryRun = false;
}
