package org.apache.fineract.export.read;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Reads all matching rows in chunks using keyset pagination on (STARTED_AT, ID): each chunk continues after the
 * last row of the previous one instead of using an OFFSET, so every chunk costs the same however deep the export is.
 * Rows without STARTED_AT are read in a second pass ordered by ID (only when no date range is set, since a date
 * range excludes them).
 */
public abstract class KeysetReader<T> {

    private final int chunkSize;

    protected KeysetReader(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        this.chunkSize = chunkSize;
    }

    /**
     * Rows with a STARTED_AT, in keyset order, after the given cursor.
     * @param lastStartedAt STARTED_AT of the last row already read, or null for the first chunk
     * @param lastId ID of the last row already read, or null for the first chunk
     */
    protected abstract List<T> fetchDated(Date lastStartedAt, Long lastId, int limit);

    /**
     * Rows without a STARTED_AT, in ID order, after the given ID (null for the first chunk).
     */
    protected abstract List<T> fetchUndated(Long lastId, int limit);

    protected abstract boolean includesUndated();

    protected abstract Date startedAt(T row);

    protected abstract Long id(T row);

    /**
     * Passes every matching row to the consumer, one chunk at a time.
     * @return the number of rows read
     */
    public long read(ChunkConsumer<T> consumer) throws IOException {
        long total = 0;
        Date lastStartedAt = null;
        Long lastId = null;
        List<T> chunk;
        do {
            chunk = fetchDated(lastStartedAt, lastId, chunkSize);
            if (!chunk.isEmpty()) {
                T last = chunk.get(chunk.size() - 1);
                lastStartedAt = startedAt(last);
                lastId = id(last);
                consumer.accept(chunk);
                total += chunk.size();
            }
        } while (chunk.size() == chunkSize);

        if (includesUndated()) {
            lastId = null;
            do {
                chunk = fetchUndated(lastId, chunkSize);
                if (!chunk.isEmpty()) {
                    lastId = id(chunk.get(chunk.size() - 1));
                    consumer.accept(chunk);
                    total += chunk.size();
                }
            } while (chunk.size() == chunkSize);
        }
        return total;
    }

    @FunctionalInterface
    public interface ChunkConsumer<T> {
        void accept(List<T> chunk) throws IOException;
    }
}
