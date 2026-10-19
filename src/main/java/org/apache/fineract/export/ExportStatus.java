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

    public static final Set<ExportStatus> ACTIVE = EnumSet.of(QUEUED, RUNNING);
}
