package org.apache.fineract.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.operations.Filter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stores an export's criteria and data scope as JSON on the job, so the worker runs exactly what was validated and
 * the job stays auditable even if the user's assignments change later.
 */
@Component
public class ExportCriteriaCodec {

    private final ObjectMapper json = new ObjectMapper();

    public String filtersToJson(ExportCriteria criteria) {
        StoredFilters stored = new StoredFilters();
        stored.startFrom = criteria.getStartFrom() == null ? null : criteria.getStartFrom().getTime();
        stored.startTo = criteria.getStartTo() == null ? null : criteria.getStartTo().getTime();
        stored.direction = criteria.getDirection();
        stored.filters = criteria.getFilters();
        return write(stored);
    }

    public String scopeToJson(DataScope scope) {
        StoredScope stored = new StoredScope();
        stored.payeePartyIds = scope.getPayeePartyIds();
        stored.currencies = scope.getCurrencies();
        stored.payeePartyIdTypes = scope.getPayeePartyIdTypes();
        return write(stored);
    }

    public ExportCriteria fromJson(String filtersJson, String scopeJson) {
        try {
            StoredFilters filters = json.readValue(filtersJson, StoredFilters.class);
            StoredScope scope = json.readValue(scopeJson, StoredScope.class);
            return ExportCriteria.builder()
                    .startFrom(filters.startFrom == null ? null : new Date(filters.startFrom))
                    .startTo(filters.startTo == null ? null : new Date(filters.startTo))
                    .direction(filters.direction == null ? Sort.Direction.DESC : filters.direction)
                    .filters(filters.filters == null ? new EnumMap<>(Filter.class) : filters.filters)
                    .scope(DataScope.builder()
                            .payeePartyIds(scope.payeePartyIds)
                            .currencies(scope.currencies)
                            .payeePartyIdTypes(scope.payeePartyIdTypes)
                            .build())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Stored export criteria cannot be read", e);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Export criteria cannot be stored", e);
        }
    }

    static class StoredFilters {
        public Long startFrom;
        public Long startTo;
        public Sort.Direction direction;
        public Map<Filter, List<String>> filters;
    }

    /** JSON shape of the data_scope column; a null list means unrestricted. */
    static class StoredScope {
        public List<String> payeePartyIds;
        public List<String> currencies;
        public List<String> payeePartyIdTypes;
    }
}
