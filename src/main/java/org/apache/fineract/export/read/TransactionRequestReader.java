package org.apache.fineract.export.read;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.TransactionRequestState;
import org.apache.fineract.operations.TransactionRequest_;
import org.apache.fineract.operations.Variable;
import org.apache.fineract.operations.Variable_;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom_;

import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionRequestReader extends JpaKeysetReader<TransactionRequest> {

    static final String ERROR_DESCRIPTION_VARIABLE = "errorDescription";


    /**
     * FD-1737: variables is partitioned by month on TIMESTAMP, so a bound on the variable rows lets MySQL skip
     * whole partitions. The variables of a request are written between its start and its end; the slack covers
     * clock skew before the start and a failure reported days later by a timer.
     */
    static final long VARIABLE_BEFORE_START_MS = TimeUnit.DAYS.toMillis(1);
    static final long VARIABLE_AFTER_START_MS = TimeUnit.DAYS.toMillis(7);

    private final ExportCriteria criteria;

    public TransactionRequestReader(EntityManagerFactory entityManagerFactory, ExportCriteria criteria, int chunkSize) {
        super(entityManagerFactory, TransactionRequest.class, criteria, TransactionRequest_.startedAt, chunkSize);
        this.criteria = criteria;
    }

    @Override
    protected Date startedAt(TransactionRequest row) {
        return row.getStartedAt();
    }

    @Override
    protected Predicate filterPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<TransactionRequest> root,
                                        Filter filter, List<String> values) {
        switch (filter) {
            case TRANSACTIONID:
                return root.get(TransactionRequest_.transactionId).in(values);
            case PAYERID:
                return root.get(TransactionRequest_.payerPartyId).in(values);
            case PAYEEID:
                return root.get(TransactionRequest_.payeePartyId).in(values);
            case WORKFLOWINSTANCEKEY:
                return root.get(TransactionRequest_.workflowInstanceKey).in(values);
            case STATE:
                return root.get(TransactionRequest_.state).in(values.stream()
                        .map(v -> TransactionRequestState.valueOf(v.toUpperCase())).collect(Collectors.toList()));
            case EXTERNALID:
                return root.get(TransactionRequest_.externalId).in(values);
            case CLIENTCORRELATIONID:
                return root.get(TransactionRequest_.clientCorrelationId).in(values);
            case PAYERDFSPID:
                return root.get(TransactionRequest_.amsBusinessShortCode).in(values);
            case ERRORDESCRIPTION:
                return hasErrorDescription(cb, query, root, values);
            default:
                throw new IllegalArgumentException("Filter " + filter + " is not supported for transaction requests");
        }
    }

    @Override
    protected List<Predicate> scopePredicates(CriteriaBuilder cb, Root<TransactionRequest> root, DataScope scope) {
        List<Predicate> predicates = new ArrayList<>();
        addScope(cb, predicates, root.get(TransactionRequest_.payeePartyId), scope.getPayeePartyIds());
        addScope(cb, predicates, root.get(TransactionRequest_.currency), scope.getCurrencies());
        addScope(cb, predicates, root.get(TransactionRequest_.payeePartyIdType), scope.getPayeePartyIdTypes());
        return predicates;
    }

    /**
     * EXISTS on the errorDescription variable. A subquery, not a join, so a request with several matching variables
     * is still exported once. Variable values are stored as JSON, so both the plain value and its JSON string
     * encoding (in quotes, with quotes and backslashes escaped) match.
     */
    private Predicate hasErrorDescription(CriteriaBuilder cb, CriteriaQuery<?> query, Root<TransactionRequest> root,
                                          List<String> values) {
        List<String> matches = new ArrayList<>(values);
        values.forEach(v -> matches.add(toJsonString(v)));

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Variable> variable = subquery.from(Variable.class);
        List<Predicate> where = new ArrayList<>(Arrays.asList(
                cb.equal(variable.get(Variable_.transactionRequest), root),
                cb.equal(variable.get(Variable_.name), ERROR_DESCRIPTION_VARIABLE),
                variable.get(Variable_.value).in(matches)));
        if (criteria.getStartFrom() != null) {
            where.add(cb.ge(variable.get(Variable_.timestamp), criteria.getStartFrom().getTime() - VARIABLE_BEFORE_START_MS));
        }
        if (criteria.getStartTo() != null) {
            where.add(cb.le(variable.get(Variable_.timestamp), criteria.getStartTo().getTime() + VARIABLE_AFTER_START_MS));
        }
        subquery.select(variable.get(AbstractPersistableCustom_.id)).where(where.toArray(new Predicate[0]));
        return cb.exists(subquery);
    }

    private static String toJsonString(String value) {
        return "\"" + new String(JsonStringEncoder.getInstance().quoteAsString(value)) + "\"";
    }
}
