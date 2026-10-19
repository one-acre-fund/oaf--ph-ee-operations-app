package org.apache.fineract.export.read;

import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.TransferStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferReaderTest {

    private static EntityManagerFactory factory;
    private EntityManager entityManager;

    @BeforeAll
    static void startDatabase() {
        factory = H2Entities.create("transfer_reader");
    }

    @AfterAll
    static void stopDatabase() {
        factory.close();
    }

    @BeforeEach
    void insertRows() {
        H2Entities.inTransaction(factory, em -> {
            em.createQuery("DELETE FROM Transfer").executeUpdate();
            // flush after each insert so IDs follow creation order (they break timestamp ties)
            for (Transfer transfer : Arrays.asList(
                    transfer("t1", 100, TransferStatus.COMPLETED, "KES", "payer-a"),
                    transfer("t2", 200, TransferStatus.FAILED, "KES", "payer-a"),
                    transfer("t3", 200, TransferStatus.COMPLETED, "UGX", "payer-b"),
                    transfer("t4", 200, TransferStatus.COMPLETED, "KES", "payer-b"),
                    transfer("t5", 300, TransferStatus.IN_PROGRESS, "KES", "payer-a"),
                    transfer("t6", null, TransferStatus.UNKNOWN, "KES", "payer-a"),
                    transfer("t7", null, TransferStatus.UNKNOWN, "KES", "payer-a"))) {
                em.persist(transfer);
                em.flush();
            }
        });
        entityManager = factory.createEntityManager();
    }

    @AfterEach
    void closeEntityManager() {
        entityManager.close();
    }

    @Test
    @DisplayName("Reads every row once, newest first, then rows without a start date")
    void readsAllRowsDescending() throws IOException {
        assertEquals(Arrays.asList("t5", "t4", "t3", "t2", "t1", "t7", "t6"), read(ExportCriteria.builder().build()));
    }

    @Test
    @DisplayName("Reads oldest first when asked")
    void readsAllRowsAscending() throws IOException {
        ExportCriteria criteria = ExportCriteria.builder().direction(Sort.Direction.ASC).build();

        assertEquals(Arrays.asList("t1", "t2", "t3", "t4", "t5", "t6", "t7"), read(criteria));
    }

    @Test
    @DisplayName("A date range is inclusive and excludes rows without a start date")
    void appliesDateRange() throws IOException {
        ExportCriteria criteria = ExportCriteria.builder().startFrom(new Date(200_000)).startTo(new Date(300_000))
                .build();

        assertEquals(Arrays.asList("t5", "t4", "t3", "t2"), read(criteria));
    }

    @Test
    @DisplayName("Applies filters, including numeric workflow keys and status names")
    void appliesFilters() throws IOException {
        ExportCriteria byStateAndPayer = ExportCriteria.builder()
                .filters(filters(Filter.STATE, Arrays.asList("completed"), Filter.PAYERID, Arrays.asList("payer-b")))
                .build();
        ExportCriteria byWorkflowKey = ExportCriteria.builder()
                .filters(Collections.singletonMap(Filter.WORKFLOWINSTANCEKEY, Arrays.asList("2", "5")))
                .build();

        assertEquals(Arrays.asList("t4", "t3"), read(byStateAndPayer));
        assertEquals(Arrays.asList("t5", "t2"), read(byWorkflowKey));
    }

    @Test
    @DisplayName("Restricts rows to the data scope; an empty scope list matches nothing")
    void appliesDataScope() throws IOException {
        ExportCriteria ugxOnly = ExportCriteria.builder()
                .scope(DataScope.builder().currencies(Collections.singletonList("UGX")).build()).build();
        ExportCriteria nothing = ExportCriteria.builder()
                .scope(DataScope.builder().payeePartyIds(Collections.emptyList()).build()).build();

        assertEquals(Collections.singletonList("t3"), read(ugxOnly));
        assertEquals(Collections.emptyList(), read(nothing));
    }

    @Test
    @DisplayName("Rejects filters that do not exist on transfers")
    void rejectsUnsupportedFilter() {
        ExportCriteria criteria = ExportCriteria.builder()
                .filters(Collections.singletonMap(Filter.EXTERNALID, Collections.singletonList("x"))).build();

        assertThrows(IllegalArgumentException.class, () -> read(criteria));
    }

    private List<String> read(ExportCriteria criteria) throws IOException {
        List<String> ids = new ArrayList<>();
        new TransferReader(entityManager, criteria, 2).read(chunk -> chunk.forEach(t -> ids.add(t.getTransactionId())));
        return ids;
    }

    private static java.util.Map<Filter, List<String>> filters(Filter f1, List<String> v1, Filter f2, List<String> v2) {
        java.util.Map<Filter, List<String>> filters = new java.util.EnumMap<>(Filter.class);
        filters.put(f1, v1);
        filters.put(f2, v2);
        return filters;
    }

    private static Transfer transfer(String transactionId, Integer startedAtSeconds, TransferStatus status,
                                     String currency, String payerPartyId) {
        Transfer transfer = new Transfer();
        transfer.setTransactionId(transactionId);
        transfer.setWorkflowInstanceKey(Long.valueOf(transactionId.substring(1)));
        transfer.setZeebeGeneration(0L);
        transfer.setStartedAt(startedAtSeconds == null ? null : new Date(startedAtSeconds * 1000L));
        transfer.setStatus(status);
        transfer.setCurrency(currency);
        transfer.setPayerPartyId(payerPartyId);
        transfer.setPayeePartyId("duka-1");
        return transfer;
    }
}
