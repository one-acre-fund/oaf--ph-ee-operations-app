package org.apache.fineract.export.read;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeysetReaderTest {

    @Test
    @DisplayName("Reads every dated row once, newest first, across chunk boundaries with equal timestamps")
    void readsAllRowsAcrossTies() throws IOException {
        InMemoryReader reader = new InMemoryReader(2, true,
                row(1, 100), row(2, 200), row(3, 200), row(4, 200), row(5, 300));

        List<Long> ids = readIds(reader);

        assertEquals(Arrays.asList(5L, 4L, 3L, 2L, 1L), ids);
        assertEquals(Arrays.asList(2, 2, 1), reader.chunkSizes);
    }

    @Test
    @DisplayName("Reads undated rows after dated ones when no date range is set")
    void readsUndatedRowsLast() throws IOException {
        InMemoryReader reader = new InMemoryReader(2, true, row(1, 100), row(2, null), row(3, null), row(4, 50));

        assertEquals(Arrays.asList(1L, 4L, 3L, 2L), readIds(reader));
    }

    @Test
    @DisplayName("Skips the undated pass when it does not apply")
    void skipsUndatedRows() throws IOException {
        InMemoryReader reader = new InMemoryReader(2, false, row(1, 100), row(2, null));

        assertEquals(Arrays.asList(1L), readIds(reader));
    }

    @Test
    @DisplayName("Stops after an exactly full last chunk with one empty query")
    void exactMultipleOfChunkSize() throws IOException {
        InMemoryReader reader = new InMemoryReader(2, false, row(1, 100), row(2, 200), row(3, 300), row(4, 400));

        assertEquals(4, reader.read(chunk -> { }));
        assertEquals(Arrays.asList(2, 2, 0), reader.chunkSizes);
    }

    @Test
    @DisplayName("Rejects a non-positive chunk size")
    void rejectsBadChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryReader(0, false));
    }

    private static List<Long> readIds(InMemoryReader reader) throws IOException {
        List<Long> ids = new ArrayList<>();
        long total = reader.read(chunk -> chunk.forEach(r -> ids.add(r.id)));
        assertEquals(ids.size(), total);
        return ids;
    }

    private static Row row(long id, Integer startedAt) {
        return new Row(id, startedAt == null ? null : new Date(startedAt));
    }

    private static final class Row {
        private final long id;
        private final Date startedAt;

        private Row(long id, Date startedAt) {
            this.id = id;
            this.startedAt = startedAt;
        }
    }

    /** Applies the same seek conditions as the SQL in JpaKeysetReader (descending order). */
    private static final class InMemoryReader extends KeysetReader<Row> {
        private final List<Row> rows;
        private final boolean undated;
        private final List<Integer> chunkSizes = new ArrayList<>();

        private InMemoryReader(int chunkSize, boolean undated, Row... rows) {
            super(chunkSize);
            this.rows = Arrays.asList(rows);
            this.undated = undated;
        }

        @Override
        protected List<Row> fetchDated(Date lastStartedAt, Long lastId, int limit) {
            List<Row> chunk = rows.stream()
                    .filter(r -> r.startedAt != null)
                    .filter(r -> lastStartedAt == null || r.startedAt.before(lastStartedAt)
                            || (r.startedAt.equals(lastStartedAt) && r.id < lastId))
                    .sorted(Comparator.comparing((Row r) -> r.startedAt).thenComparing(r -> r.id).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
            chunkSizes.add(chunk.size());
            return chunk;
        }

        @Override
        protected List<Row> fetchUndated(Long lastId, int limit) {
            return rows.stream()
                    .filter(r -> r.startedAt == null && (lastId == null || r.id < lastId))
                    .sorted(Comparator.comparing((Row r) -> r.id).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        protected boolean includesUndated() {
            return undated;
        }

        @Override
        protected Date startedAt(Row row) {
            return row.startedAt;
        }

        @Override
        protected Long id(Row row) {
            return row.id;
        }
    }
}
