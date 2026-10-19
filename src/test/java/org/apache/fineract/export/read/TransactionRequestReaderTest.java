package org.apache.fineract.export.read;

import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.TransactionRequestState;
import org.apache.fineract.operations.Variable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionRequestReaderTest {

    private static EntityManagerFactory factory;

    @BeforeAll
    static void startDatabase() {
        factory = H2Entities.create("transaction_request_reader");
    }

    @AfterAll
    static void stopDatabase() {
        factory.close();
    }

    @BeforeEach
    void insertRows() {
        H2Entities.inTransaction(factory, em -> {
            em.createQuery("DELETE FROM Variable").executeUpdate();
            em.createQuery("DELETE FROM TransactionRequest").executeUpdate();
            // flush after each insert so IDs follow creation order (they break timestamp ties)
            for (TransactionRequest request : Arrays.asList(
                    request("r1", 100, TransactionRequestState.ACCEPTED, "mno-1", "KES"),
                    request("r2", 200, TransactionRequestState.FAILED, "mno-2", "KES"),
                    request("r3", 200, TransactionRequestState.FAILED, "mno-3", "UGX"),
                    request("r4", null, TransactionRequestState.IN_PROGRESS, null, "KES"),
                    request("r5", 50, TransactionRequestState.FAILED, "mno-5", "KES"),
                    request("r6", 40, TransactionRequestState.FAILED, "mno-6", "KES"))) {
                em.persist(request);
                em.flush();
            }
            // values are stored as JSON, so with quotes; r2 has the same error twice
            em.persist(variable(2, "errorDescription", "\"Insufficient funds\""));
            em.persist(variable(2, "errorDescription", "\"Insufficient funds\""));
            em.persist(variable(3, "errorDescription", "\"Timeout\""));
            em.persist(variable(1, "note", "\"Insufficient funds\""));
            // JSON encoding of: Unknown account "abc"   and   Bad path C:\temp
            em.persist(variable(5, "errorDescription", "\"Unknown account \\\"abc\\\"\""));
            em.persist(variable(6, "errorDescription", "\"Bad path C:\\\\temp\""));
        });
    }

    @Test
    @DisplayName("Reads every request once, newest first, then requests without a start date")
    void readsAllRows() throws IOException {
        assertEquals(Arrays.asList("r3", "r2", "r1", "r5", "r6", "r4"), read(ExportCriteria.builder().build()));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("singleFilters")
    @DisplayName("Applies each filter on its own column")
    void appliesEachFilter(Filter filter, List<String> values, List<String> expected) throws IOException {
        assertEquals(expected, read(criteria(filter, values)));
    }

    static Stream<Arguments> singleFilters() {
        return Stream.of(
                Arguments.of(Filter.TRANSACTIONID, Arrays.asList("r1", "r3"), Arrays.asList("r3", "r1")),
                Arguments.of(Filter.PAYERID, Collections.singletonList("payer-r2"), Collections.singletonList("r2")),
                Arguments.of(Filter.PAYEEID, Collections.singletonList("duka-2"), Collections.singletonList("r3")),
                Arguments.of(Filter.WORKFLOWINSTANCEKEY, Arrays.asList("2", "4"), Arrays.asList("r2", "r4")),
                Arguments.of(Filter.CLIENTCORRELATIONID, Collections.singletonList("cc-r4"),
                        Collections.singletonList("r4")),
                Arguments.of(Filter.PAYERDFSPID, Collections.singletonList("ams-r1"), Collections.singletonList("r1")));
    }

    @Test
    @DisplayName("Filters by state names and external ids")
    void filtersByStateAndExternalId() throws IOException {
        assertEquals(Arrays.asList("r3", "r2", "r5", "r6"),
                read(criteria(Filter.STATE, Collections.singletonList("failed"))));
        assertEquals(Collections.singletonList("r1"),
                read(criteria(Filter.EXTERNALID, Collections.singletonList("mno-1"))));
    }

    @Test
    @DisplayName("Filters by error description once per request, matching the stored JSON value")
    void filtersByErrorDescription() throws IOException {
        assertEquals(Collections.singletonList("r2"),
                read(criteria(Filter.ERRORDESCRIPTION, Collections.singletonList("Insufficient funds"))));
        assertEquals(Arrays.asList("r3", "r2"),
                read(criteria(Filter.ERRORDESCRIPTION, Arrays.asList("Insufficient funds", "Timeout"))));
    }

    @Test
    @DisplayName("Matches error descriptions containing quotes and backslashes against their JSON encoding")
    void filtersByErrorDescriptionWithEscapes() throws IOException {
        assertEquals(Collections.singletonList("r5"),
                read(criteria(Filter.ERRORDESCRIPTION, Collections.singletonList("Unknown account \"abc\""))));
        assertEquals(Collections.singletonList("r6"),
                read(criteria(Filter.ERRORDESCRIPTION, Collections.singletonList("Bad path C:\\temp"))));
    }

    @Test
    @DisplayName("Restricts requests to the data scope")
    void appliesDataScope() throws IOException {
        ExportCriteria kesOnly = ExportCriteria.builder()
                .scope(DataScope.builder().currencies(Collections.singletonList("KES")).build()).build();

        assertEquals(Arrays.asList("r2", "r1", "r5", "r6", "r4"), read(kesOnly));
    }

    private List<String> read(ExportCriteria criteria) throws IOException {
        List<String> ids = new ArrayList<>();
        new TransactionRequestReader(factory, criteria, 2)
                .read(chunk -> chunk.forEach(r -> ids.add(r.getTransactionId())));
        return ids;
    }

    private static ExportCriteria criteria(Filter filter, List<String> values) {
        return ExportCriteria.builder().filters(Collections.singletonMap(filter, values)).build();
    }

    private static TransactionRequest request(String transactionId, Integer startedAtSeconds,
                                              TransactionRequestState state, String externalId, String currency) {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId(transactionId);
        request.setWorkflowInstanceKey(transactionId.substring(1));
        request.setZeebeGeneration(0L);
        request.setStartedAt(startedAtSeconds == null ? null : new Date(startedAtSeconds * 1000L));
        request.setState(state);
        request.setExternalId(externalId);
        request.setCurrency(currency);
        request.setPayeePartyId("r3".equals(transactionId) ? "duka-2" : "duka-1");
        request.setPayerPartyId("payer-" + transactionId);
        request.setClientCorrelationId("cc-" + transactionId);
        request.setAmsBusinessShortCode("ams-" + transactionId);
        return request;
    }

    private static Variable variable(long workflowInstanceKey, String name, String value) {
        Variable variable = new Variable();
        variable.setWorkflowInstanceKey(workflowInstanceKey);
        variable.setZeebeGeneration(0L);
        variable.setName(name);
        variable.setValue(value);
        return variable;
    }
}
