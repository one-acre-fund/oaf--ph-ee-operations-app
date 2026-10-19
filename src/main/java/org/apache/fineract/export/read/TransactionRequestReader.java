package org.apache.fineract.export.read;

import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.TransactionRequestState;
import org.apache.fineract.operations.TransactionRequest_;
import org.apache.fineract.operations.Variable;
import org.apache.fineract.operations.Variable_;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom_;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionRequestReader extends JpaKeysetReader<TransactionRequest> {

    static final String ERROR_DESCRIPTION_VARIABLE = "errorDescription";

    public TransactionRequestReader(EntityManager entityManager, ExportCriteria criteria, int chunkSize) {
        super(entityManager, TransactionRequest.class, criteria, TransactionRequest_.startedAt, chunkSize);
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
     * is still exported once. Variable values are stored as JSON, so both the plain and the quoted value match.
     */
    private Predicate hasErrorDescription(CriteriaBuilder cb, CriteriaQuery<?> query, Root<TransactionRequest> root,
                                          List<String> values) {
        List<String> matches = new ArrayList<>(values);
        values.forEach(v -> matches.add("\"" + v + "\""));

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Variable> variable = subquery.from(Variable.class);
        subquery.select(variable.get(AbstractPersistableCustom_.id)).where(
                cb.equal(variable.get(Variable_.transactionRequest), root),
                cb.equal(variable.get(Variable_.name), ERROR_DESCRIPTION_VARIABLE),
                variable.get(Variable_.value).in(matches));
        return cb.exists(subquery);
    }
}
