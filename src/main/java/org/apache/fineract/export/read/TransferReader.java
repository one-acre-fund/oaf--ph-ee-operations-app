package org.apache.fineract.export.read;

import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.TransferStatus;
import org.apache.fineract.operations.Transfer_;

import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TransferReader extends JpaKeysetReader<Transfer> {

    public TransferReader(EntityManagerFactory entityManagerFactory, ExportCriteria criteria, int chunkSize) {
        super(entityManagerFactory, Transfer.class, criteria, Transfer_.startedAt, chunkSize);
    }

    @Override
    protected Date startedAt(Transfer row) {
        return row.getStartedAt();
    }

    @Override
    protected Predicate filterPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Transfer> root,
                                        Filter filter, List<String> values) {
        switch (filter) {
            case TRANSACTIONID:
                return root.get(Transfer_.transactionId).in(values);
            case PAYERID:
                return root.get(Transfer_.payerPartyId).in(values);
            case PAYEEID:
                return root.get(Transfer_.payeePartyId).in(values);
            case WORKFLOWINSTANCEKEY:
                return root.get(Transfer_.workflowInstanceKey)
                        .in(values.stream().map(Long::valueOf).collect(Collectors.toList()));
            case STATE:
                return root.get(Transfer_.status)
                        .in(values.stream().map(v -> TransferStatus.valueOf(v.toUpperCase())).collect(Collectors.toList()));
            case CLIENTCORRELATIONID:
                return root.get(Transfer_.clientCorrelationId).in(values);
            case PAYERDFSPID:
                return root.get(Transfer_.amsBusinessShortCode).in(values);
            default:
                throw new IllegalArgumentException("Filter " + filter + " is not supported for transfers");
        }
    }

    @Override
    protected List<Predicate> scopePredicates(CriteriaBuilder cb, Root<Transfer> root, DataScope scope) {
        List<Predicate> predicates = new ArrayList<>();
        addScope(cb, predicates, root.get(Transfer_.payeePartyId), scope.getPayeePartyIds());
        addScope(cb, predicates, root.get(Transfer_.currency), scope.getCurrencies());
        addScope(cb, predicates, root.get(Transfer_.payeePartyIdType), scope.getPayeePartyIdTypes());
        return predicates;
    }
}
