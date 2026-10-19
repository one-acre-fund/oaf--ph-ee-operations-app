package org.apache.fineract.export;

import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserScopeResolverTest {

    private final UserScopeResolver resolver = new UserScopeResolver();

    @Test
    @DisplayName("Transfers are not scoped, as in the legacy transfers export")
    void transfersUnrestricted() {
        assertSame(DataScope.UNRESTRICTED, resolver.resolve(user(null, null, null), ExportType.TRANSFERS));
    }

    @Test
    @DisplayName("Transaction requests use the user's assignments; [\"*\"] means all")
    void transactionRequestsScoped() {
        DataScope scope = resolver.resolve(user(Arrays.asList("duka-1", "duka-2"), Collections.singletonList("*"),
                Collections.singletonList("MSISDN")), ExportType.TRANSACTION_REQUESTS);

        assertEquals(Arrays.asList("duka-1", "duka-2"), scope.getPayeePartyIds());
        assertNull(scope.getCurrencies());
        assertEquals(Collections.singletonList("MSISDN"), scope.getPayeePartyIdTypes());
    }

    @Test
    @DisplayName("A user with an empty assignment is refused (403) instead of exporting everything")
    void emptyAssignmentRefused() {
        ExportException e = assertThrows(ExportException.class, () -> resolver.resolve(
                user(Collections.singletonList("*"), Collections.emptyList(), Collections.singletonList("*")),
                ExportType.TRANSACTION_REQUESTS));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    @DisplayName("Missing dukas or id types are refused too")
    void otherEmptyAssignmentsRefused() {
        assertThrows(ExportException.class, () -> resolver.resolve(user(Collections.emptyList(),
                Collections.singletonList("*"), Collections.singletonList("*")), ExportType.TRANSACTION_REQUESTS));
        assertThrows(ExportException.class, () -> resolver.resolve(user(Collections.singletonList("*"),
                Collections.singletonList("*"), Collections.emptyList()), ExportType.TRANSACTION_REQUESTS));
    }

    private static AppUser user(List<String> dukas, List<String> currencies, List<String> idTypes) {
        AppUser user = new AppUser();
        user.setPayeePartyIdsList(dukas);
        user.setCurrenciesList(currencies);
        user.setPayeePartyIdTypesList(idTypes);
        return user;
    }
}
