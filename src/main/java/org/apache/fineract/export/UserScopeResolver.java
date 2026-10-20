package org.apache.fineract.export;

import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.organisation.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserScopeResolver {

    /**
     * Transfers are not scoped, as in the legacy transfers export. Transaction requests use the user's dukas,
     * currencies and payee id types; an empty assignment is refused (deny by default) instead of exporting
     * everything.
     */
    public DataScope resolve(AppUser user, ExportType type) {
        if (type == ExportType.TRANSFERS) {
            return DataScope.UNRESTRICTED;
        }
        DataScope scope = DataScope.builder()
                .payeePartyIds(DataScope.restriction(user.getPayeePartyIdsList()))
                .currencies(DataScope.restriction(user.getCurrenciesList()))
                .payeePartyIdTypes(DataScope.restriction(user.getPayeePartyIdTypesList()))
                .build();
        if (isEmpty(scope.getPayeePartyIds()) || isEmpty(scope.getCurrencies())
                || isEmpty(scope.getPayeePartyIdTypes())) {
            throw new ExportException(HttpStatus.FORBIDDEN,
                    "User has no data assignments (dukas, currencies or payee id types) to export");
        }
        return scope;
    }

    private static boolean isEmpty(List<String> restriction) {
        return restriction != null && restriction.isEmpty();
    }
}
