package org.apache.fineract.export;

import java.util.EnumSet;
import java.util.Set;

public enum ExportStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    DOWNLOADED,
    EXPIRED;

    /**
     * @return a new set of the statuses of jobs that are not finished yet
     */
    public static Set<ExportStatus> active() {
        return EnumSet.of(QUEUED, RUNNING);
    }
}
