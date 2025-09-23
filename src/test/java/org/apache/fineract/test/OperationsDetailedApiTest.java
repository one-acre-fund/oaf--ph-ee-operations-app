package org.apache.fineract.test;

import org.apache.fineract.TestUtils;
import org.apache.fineract.api.OperationsDetailedApi;
import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.TransactionRequestRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

class OperationsDetailedApiTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private TransactionRequestRepository transactionRequestRepository;

    @InjectMocks
    private OperationsDetailedApi operationsDetailedApi;
    private TestUtils testUtils = new TestUtils();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void testTransactionRequestFilter_UserNotAuthenticated() {
        // Arrange
        List<Specifications<TransactionRequest>> specs = Collections.emptyList();
        PageRequest pager = new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "startedAt"));
        String currency = "USD";
        String payeePartyId = "12345";
        String payeePartyIdType = "MNO";

        // Arrange
        // Set up specs, sortedBy, sortedOrder, page, size, currency, payeePartyId, etc.
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act
        Page<TransactionRequest> result = operationsDetailedApi.transactionRequestFilter(pager, specs, currency, payeePartyId, payeePartyIdType);

        // Assert
        verifyZeroInteractions(appUserRepository, transactionRequestRepository);
        assertEquals(0, result.getTotalElements());
        // Add more assertions based on your logic when the user is not authenticated
    }

    @Test
    void testTransactionRequestFilter_UserNotAuthorizedForCurrenciesOrPayeePartyIdsOrPayeePartyIdTypes() {
        List<Specifications<TransactionRequest>> specs = Collections.emptyList();
        String currency = "USD";
        String payeePartyId = "12345";
        String payeePartyIdType = "MNO";
        PageRequest pager = new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "startedAt"));


        AppUser currentUser = new AppUser(); // User not authorized for anything
        when(appUserRepository.findAppUserByName(any())).thenReturn(currentUser);

        testUtils.setupSecurityContext(currentUser);
        // Act
        Page<TransactionRequest> result = operationsDetailedApi.transactionRequestFilter(pager, specs, currency, payeePartyId, payeePartyIdType);

        // Assert
        verify(appUserRepository, times(1)).findAppUserByName(any());
        verify(transactionRequestRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void testCheckUserPayeePartyIdTypesAssigned_AssignedNone() {
        AppUser currentUser = mock(AppUser.class);
        String payeePartyIdType = "some_type";
        when(currentUser.getPayeePartyIdTypesList()).thenReturn(new ArrayList<>());

        // Act
        List<Specifications<TransactionRequest>> result = operationsDetailedApi.checkUserPayeePartyIdTypesAssigned(currentUser, payeePartyIdType);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void testCheckUserPayeePartyIdsAssigned_AssignedNone() {
        AppUser currentUser = mock(AppUser.class);
        String payeePartyId = "12345";
        when(currentUser.getPayeePartyIdTypesList()).thenReturn(new ArrayList<>());

        // Act
        List<Specifications<TransactionRequest>> result = operationsDetailedApi.checkUserDukasAssigned(currentUser, payeePartyId);

        // Assert
        assertEquals(1, result.size());
    }
    @Test
    void testCheckUserCurrenciesAssigned_AssignedNone() {
        AppUser currentUser = mock(AppUser.class);
        String currency = "12345";
        when(currentUser.getPayeePartyIdTypesList()).thenReturn(new ArrayList<>());

        // Act
        List<Specifications<TransactionRequest>> result = operationsDetailedApi.checkUserCurrenciesAssigned(currentUser, currency);

        // Assert
        assertEquals(1, result.size());
    }
}

