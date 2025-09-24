package org.apache.fineract.operations;

import org.springframework.data.jpa.domain.Specifications;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

/**
 * Contains methods for common specification operations
 */
public class Specs {

    private Specs() {}

    /**
     * Creates specification for IN clause
     * @param attribute attribute to be checked
     * @param inputs list of inputs
     * @return Specifications
     * @param <T> type of the entity
     * @param <U> type of the attribute
     */
    public static <T, U> Specifications<T> in(SingularAttribute<T, U> attribute, List<U> inputs) {
        return where(((root, query, cb) -> {
            final Path<U> group = root.get(attribute);
            CriteriaBuilder.In<U> cr = cb.in(group);
            for(U input: inputs ) {
                cr.value(input);
            }
            return cr;
        }));
    }
}
