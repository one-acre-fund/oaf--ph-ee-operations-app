package org.apache.fineract.retention;

import java.time.YearMonth;
import java.util.List;

/** The statements the retention job runs against one tenant's variables table; a seam for tests. */
interface VariableTableOps {

    /** Non-blocking per-tenant lock, so two pods never archive or alter the same table at once. */
    boolean tryLock(String tenant);

    void unlock(String tenant);

    /** Partition names in order, empty when the table is not partitioned (not cut over yet). */
    List<String> partitionNames();

    long rowCount(String partition);

    /** Splits the empty tail of pmax so the month gets its own partition. */
    void addMonth(YearMonth month);

    void dropPartition(String partition);
}
