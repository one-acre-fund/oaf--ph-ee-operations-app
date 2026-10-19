package org.apache.fineract.export.read;

import org.apache.fineract.operations.Filter;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom_;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Keyset reader over a JPA entity. Run it outside a transaction, so the entities of each chunk are not kept in a
 * persistence context and can be garbage collected once written.
 */
public abstract class JpaKeysetReader<T extends AbstractPersistableCustom<Long>> extends KeysetReader<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;
    private final ExportCriteria criteria;
    private final SingularAttribute<? super T, Date> startedAtAttribute;

    protected JpaKeysetReader(EntityManager entityManager, Class<T> entityClass, ExportCriteria criteria,
                              SingularAttribute<? super T, Date> startedAtAttribute, int chunkSize) {
        super(chunkSize);
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.criteria = criteria;
        this.startedAtAttribute = startedAtAttribute;
    }

    /**
     * Predicate for one filter, or IllegalArgumentException when the filter does not apply to this entity.
     */
    protected abstract Predicate filterPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root,
                                                 Filter filter, List<String> values);

    /**
     * Predicates restricting the rows to the requester's data scope.
     */
    protected abstract List<Predicate> scopePredicates(CriteriaBuilder cb, Root<T> root, DataScope scope);

    @Override
    protected List<T> fetchDated(Date lastStartedAt, Long lastId, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        Path<Date> startedAt = root.get(startedAtAttribute);
        Path<Long> id = root.get(AbstractPersistableCustom_.id);

        List<Predicate> predicates = basePredicates(cb, query, root);
        predicates.add(cb.isNotNull(startedAt));
        if (lastStartedAt != null) {
            predicates.add(after(cb, startedAt, id, lastStartedAt, lastId));
        }
        query.select(root).where(predicates.toArray(new Predicate[0]))
                .orderBy(order(cb, startedAt), order(cb, id));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    @Override
    protected List<T> fetchUndated(Long lastId, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        Path<Long> id = root.get(AbstractPersistableCustom_.id);

        List<Predicate> predicates = basePredicates(cb, query, root);
        predicates.add(cb.isNull(root.get(startedAtAttribute)));
        if (lastId != null) {
            predicates.add(descending() ? cb.lessThan(id, lastId) : cb.greaterThan(id, lastId));
        }
        query.select(root).where(predicates.toArray(new Predicate[0])).orderBy(order(cb, id));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    @Override
    protected boolean includesUndated() {
        return !criteria.hasDateRange();
    }

    @Override
    protected Long id(T row) {
        return row.getId();
    }

    /**
     * IN restriction for one data scope dimension: null means unrestricted, an empty list matches nothing.
     */
    protected static <V> void addScope(CriteriaBuilder cb, List<Predicate> predicates, Expression<V> attribute,
                                       List<V> allowed) {
        if (allowed == null) {
            return;
        }
        predicates.add(allowed.isEmpty() ? cb.disjunction() : attribute.in(allowed));
    }

    private List<Predicate> basePredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<T> root) {
        List<Predicate> predicates = new ArrayList<>();
        Path<Date> startedAt = root.get(startedAtAttribute);
        if (criteria.getStartFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(startedAt, criteria.getStartFrom()));
        }
        if (criteria.getStartTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(startedAt, criteria.getStartTo()));
        }
        for (Map.Entry<Filter, List<String>> filter : criteria.getFilters().entrySet()) {
            predicates.add(filterPredicate(cb, query, root, filter.getKey(), filter.getValue()));
        }
        predicates.addAll(scopePredicates(cb, root, criteria.getScope()));
        return predicates;
    }

    /**
     * Rows after (STARTED_AT, ID) in the sort order. Written as "ts <= last AND (ts < last OR id < lastId)" for
     * descending order, so the database can seek on the STARTED_AT index.
     */
    private Predicate after(CriteriaBuilder cb, Path<Date> startedAt, Path<Long> id, Date lastStartedAt,
                            Long lastId) {
        if (descending()) {
            return cb.and(cb.lessThanOrEqualTo(startedAt, lastStartedAt),
                    cb.or(cb.lessThan(startedAt, lastStartedAt), cb.lessThan(id, lastId)));
        }
        return cb.and(cb.greaterThanOrEqualTo(startedAt, lastStartedAt),
                cb.or(cb.greaterThan(startedAt, lastStartedAt), cb.greaterThan(id, lastId)));
    }

    private Order order(CriteriaBuilder cb, Expression<?> expression) {
        return descending() ? cb.desc(expression) : cb.asc(expression);
    }

    private boolean descending() {
        return criteria.getDirection() == Sort.Direction.DESC;
    }
}
