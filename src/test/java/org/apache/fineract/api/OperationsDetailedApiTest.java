package org.apache.fineract.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.operations.*;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.utils.DateUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OperationsDetailedApiTest {
    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRequestRepository transactionRequestRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DateUtil dateUtil;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AMSConfig amsConfig;

    @InjectMocks
    private OperationsDetailedApi operationsDetailedApi;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("Returns a list of AmsSource objects when amsSourcesString is valid JSON")
    @Test
    void test_valid_json_returns_ams_sources_list() {
        String validJson = "[{\"name\":\"Source1\",\"id\":\"1\"},{\"name\":\"Source2\",\"id\":\"2\"}]";
        AMSConfig.AmsSource amsSource1 = new AMSConfig.AmsSource();
        AMSConfig.AmsSource amsSource2 = new AMSConfig.AmsSource();



        List<AMSConfig.AmsSource> amsSources = new ArrayList<>();
        amsSources.add(amsSource1);
        amsSources.add(amsSource2);
        when(amsConfig.getAmsSourcesList()).thenReturn(amsSources);

        List<AMSConfig.AmsSource> result = operationsDetailedApi.getAmsSourcesList();

        assertEquals(2, result.size());
    }

    @DisplayName("Returns a page of TransferResponse objects when valid parameters are provided")
    @Test
    void test_returns_page_of_transfer_response_objects() throws IOException {
        // Arrange
        PageRequest pager = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startedAt"));
        Transfer transfer = new Transfer();
        transfer.setTransactionId("12345");
        transfer.setAmount(new BigDecimal("100.00"));
        transfer.setCurrency("USD");
        transfer.setStatus(TransferStatus.COMPLETED);
        Page<Transfer> transferPage = new PageImpl<>(Collections.singletonList(transfer), pager, 1);


        TransferResponse transferResponse = new TransferResponse();
        transferResponse.setAmount(new BigDecimal("100.00"));
        transferResponse.setTransactionId("12345");
        Mockito.when(transferRepository.findAll(Mockito.any(PageRequest.class)))
                .thenReturn(transferPage);
        Mockito.when(objectMapper.writeValueAsString(transfer))
                .thenReturn("{\"transactionId\":\"12345\",\"amount\":100.00,\"currency\":\"USD\",\"status\":\"COMPLETED\"}");
        Mockito.when(objectMapper.readValue(anyString(), eq(TransferResponse.class)))
                .thenReturn(transferResponse);

        // Act
        Page<TransferResponse> result = operationsDetailedApi.transfers(0, 20, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,"desc");

        // Assert
        assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("12345", result.getContent().get(0).getTransactionId());
    }

    @DisplayName("Returns a page of transaction requests when user is authenticated and has valid assignments")
    @Test
    void test_authenticated_user_with_valid_assignments() {
        // Arrange
        PageRequest pager = PageRequest.of(0, 10);
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        String currency = "USD";
        String payeePartyId = "12345";
        String payeePartyIdType = "ID";

        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setPayeePartyIdsList(Collections.singletonList("*"));
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);

        Page<TransactionRequest> expectedPage = new PageImpl<>(new ArrayList<>(), pager, 0);
        Mockito.when(transactionRequestRepository.findAll(Mockito.any(Specifications.class), Mockito.eq(pager))).thenReturn(expectedPage);


        // Act
        Page<TransactionRequest> result = operationsDetailedApi.transactionRequestFilter(pager, specs, currency, payeePartyId, payeePartyIdType);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }
    @DisplayName("Returns a page of transaction requests when user is authenticated and has valid assignments")
    @Test
    void test_authenticated_user_with_valid_assignments_no_payee_assigned() {
        // Arrange
        PageRequest pager = PageRequest.of(0, 10);
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        String currency = "USD";
        String payeePartyId = "12345";
        String payeePartyIdType = "ID";

        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);

        Page<TransactionRequest> expectedPage = new PageImpl<>(new ArrayList<>(), pager, 0);
        Mockito.when(transactionRequestRepository.findAll(Mockito.any(Specifications.class), Mockito.eq(pager))).thenReturn(expectedPage);


        // Act
        Page<TransactionRequest> result = operationsDetailedApi.transactionRequestFilter(pager, specs, currency, payeePartyId, payeePartyIdType);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPage, result);
    }

    @DisplayName("Handles null authentication object gracefully")
    @Test
    void test_null_authentication_object() {
        // Arrange
        PageRequest pager = PageRequest.of(0, 10);
        List<Specifications<TransactionRequest>> specs = new ArrayList<>();
        String currency = "USD";
        String payeePartyId = "12345";
        String payeePartyIdType = "ID";

        SecurityContextHolder.getContext().setAuthentication(null);

        OperationsDetailedApi operationsDetailedApi = new OperationsDetailedApi();

        // Act
        Page<TransactionRequest> result = operationsDetailedApi.transactionRequestFilter(pager, specs, currency, payeePartyId, payeePartyIdType);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @DisplayName("Returns specifications when currency is provided")
    @Test
    void test_returns_specifications_when_currency_provided() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String currency = "USD";

        List<Specifications<TransactionRequest>> specs = operationsDetailedApi.checkUserCurrenciesAssigned(mockUser, currency);

        Assertions.assertNotNull(specs);
        Assertions.assertEquals(1, specs.size());
        verify(mockUser, never()).getCurrenciesList();
    }

    @DisplayName("Handles null currentUser gracefully")
    @Test
    void test_handles_null_currency() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String currency = null;
        List<Specifications<TransactionRequest>> specs = operationsDetailedApi.checkUserCurrenciesAssigned(mockUser, currency);

        Assertions.assertNotNull(specs);
        verify(mockUser).getCurrenciesList();
        Assertions.assertEquals(1, specs.size());
    }

    @DisplayName("Returns specifications list with 'like' specification when payeePartyId is provided")
    @Test
    void test_returns_like_specification_when_payeePartyId_provided() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String payeePartyId = "Id123";

        List<Specifications<TransactionRequest>> result = operationsDetailedApi.checkUserDukasAssigned(mockUser, payeePartyId);

        Assertions.assertEquals(1, result.size());
        verify(mockUser, never()).getPayeePartyIdsList();
    }

    @Test
    void test_returns_like_specification_when_payeePartyId_null() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String payeePartyId = null;

        List<Specifications<TransactionRequest>> result = operationsDetailedApi.checkUserDukasAssigned(mockUser, payeePartyId);

        Assertions.assertEquals(1, result.size());
        verify(mockUser).getPayeePartyIdsList();
    }

    @DisplayName("Returns specifications list with 'like' specification when payeePartyIdType is provided")
    @Test
    void test_returns_like_specification_when_payeePartyIdType_provided() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String payeePartyIdType = "MNO";

        List<Specifications<TransactionRequest>> specs = operationsDetailedApi.checkUserPayeePartyIdTypesAssigned(mockUser, payeePartyIdType);

        Assertions.assertEquals(1, specs.size());
        verify(mockUser, never()).getPayeePartyIdTypesList();

    }
    @DisplayName("Handles null currentUser gracefully")
    @Test
    void test_returns_like_specification_when_payeePartyIdType_null() {
        AppUser mockUser = Mockito.mock(AppUser.class);
        String payeePartyIdType = null;

        List<Specifications<TransactionRequest>> specs = operationsDetailedApi.checkUserPayeePartyIdTypesAssigned(mockUser, payeePartyIdType);

        Assertions.assertEquals(1, specs.size());
        verify(mockUser).getPayeePartyIdTypesList();

    }

    @DisplayName("Filters transaction requests based on provided state")
    @Test
    void test_filter_transaction_requests_by_state() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);


        Map<String, List<String>> body = new HashMap<>();
        body.put("TRANSACTIONID", Arrays.asList("12345"));
        Page<TransactionRequest> transferPage = new PageImpl<>(Collections.singletonList(new TransactionRequest()));
        Mockito.when(transactionRequestRepository.findAll(any(Specifications.class), any(PageRequest.class))).thenReturn(transferPage);

        Map<String, String> result = operationsDetailedApi.filterTransactionRequests(response, "export", 0, 10000, "DESC", null, null, "COMPLETED", body);

        assertNotNull(result);
        assertEquals("CSV_BUILDER", result.get("errorCode"));
        assertEquals("Print writer can't be null", result.get("developerMessage"));
        verify(response,never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    @Test
    void test_filter_transaction_requests_by_states() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);


        Map<String, List<String>> body = new HashMap<>();
        body.put("TRANSACTIONID", Arrays.asList("12345"));

        Page<Transfer> transferPage = new PageImpl<>(Collections.EMPTY_LIST);
        Mockito.when(transactionRequestRepository.findAll(any(Specifications.class), any(PageRequest.class))).thenReturn(transferPage);

        Map<String, String> result = operationsDetailedApi.filterTransactionRequests(response, "export", 0, 10000, "DESC", null, null, "COMPLETED", body);

        assertNotNull(result);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Export transfers with valid filters and date range")
    @Test
    void test_export_transfers_with_valid_filters_and_date_range() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);


        Map<String, List<String>> body = new HashMap<>();
        body.put("TRANSACTIONID", Arrays.asList("12345"));
        Page<Transfer> transferPage = new PageImpl<>(Collections.singletonList(new Transfer()));
        Mockito.when(transferRepository.findAll(any(Specifications.class), any(PageRequest.class))).thenReturn(transferPage);

        Map<String, String> result = operationsDetailedApi.exportTransfers(response, 0, 10000, "DESC", "2023-01-01", "2023-12-31", "COMPLETED", body);

        assertNotNull(result);
        assertEquals("CSV_BUILDER", result.get("errorCode"));
        assertEquals("Print writer can't be null", result.get("developerMessage"));
        verify(response,never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    @Test
    void test_export_transfers_with_valid_filters_and_date_range_writes_csv() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);


        Map<String, List<String>> body = new HashMap<>();
        body.put("TRANSACTIONID", Arrays.asList("12345"));
        Page<Transfer> transferPage = new PageImpl<>(Collections.singletonList(new Transfer()));
        Mockito.when(transferRepository.findAll(any(Specifications.class), any(PageRequest.class))).thenReturn(transferPage);

        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        Map<String, String> result = operationsDetailedApi.exportTransfers(response, 0, 10000, "DESC", "2023-01-01", "2023-12-31", "COMPLETED", body);

        assertNull(result);
    }

    @Test
    void test_export_transfers_with_valid_filters_and_date_range_empty() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getName()).thenReturn("testUser");

        AppUser appUser = new AppUser();
        appUser.setCurrenciesList(Collections.singletonList("*"));
        Mockito.when(appUserRepository.findAppUserByName("testUser")).thenReturn(appUser);


        Map<String, List<String>> body = new HashMap<>();
        body.put("TRANSACTIONID", Arrays.asList("12345"));
        Page<Transfer> transferPage = new PageImpl<>(Collections.EMPTY_LIST);
        Mockito.when(transferRepository.findAll(any(Specifications.class), any(PageRequest.class))).thenReturn(transferPage);


        Map<String, String> result = operationsDetailedApi.exportTransfers(response, 0, 10000, "DESC", "2023-01-01", "2023-12-31", "COMPLETED", body);

        assertNotNull(result);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

}