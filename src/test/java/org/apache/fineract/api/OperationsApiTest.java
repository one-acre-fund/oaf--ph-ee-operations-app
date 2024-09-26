package org.apache.fineract.api;

import org.apache.fineract.operations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


class OperationsApiTest {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Mock
    private BusinessKeyRepository businessKeyRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRequestRepository transactionRequestRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private RestTemplate restTemplate;

    @Value("${channel-connector.url}")
    private String channelConnectorUrl;

    @Value("${channel-connector.transfer-path}")
    private String channelConnectorTransferPath;

    @InjectMocks
    private OperationsApi operationsApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Successfully refunds a completed incoming transfer
    @Test
    void test_successful_refund_completed_incoming_transfer() {
        // Arrange
        String tenantId = "tenant1";
        String transactionId = "txn123";
        String requestBody = "{\"comment\":\"Refund for transaction\"}";
        HttpServletResponse response = mock(HttpServletResponse.class);
        Transfer existingIncomingTransfer = new Transfer();
        existingIncomingTransfer.setTransactionId(transactionId);
        existingIncomingTransfer.setDirection("INCOMING");
        existingIncomingTransfer.setStatus(TransferStatus.COMPLETED);
        existingIncomingTransfer.setPayeePartyIdType("MSISDN");
        existingIncomingTransfer.setPayeePartyId("12345");
        existingIncomingTransfer.setPayerPartyIdType("MSISDN");
        existingIncomingTransfer.setPayerPartyId("67890");
        existingIncomingTransfer.setAmount(new BigDecimal("100.00"));
        existingIncomingTransfer.setCurrency("USD");

        when(transferRepository.findFirstByTransactionIdAndDirection(transactionId, "INCOMING"))
                .thenReturn(existingIncomingTransfer);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Platform-TenantId", tenantId);
        httpHeaders.add("Content-Type", "application/json");

        ResponseEntity<String> channelResponse = new ResponseEntity<>("{\"status\":\"success\"}", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(channelResponse);

        // Act
        String result = operationsApi.refundTransfer(tenantId, transactionId, requestBody, response);

        // Assert
        assertEquals("{\"status\":\"success\"}", result);
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @DisplayName("Incoming transfer does not exist")
    @Test
    void test_incoming_transfer_does_not_exist() {
        // Arrange
        String tenantId = "tenant1";
        String transactionId = "txn123";
        String requestBody = "{}";
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(transferRepository.findFirstByTransactionIdAndDirection(transactionId, "INCOMING"))
                .thenReturn(null);

        // Act
        String result = operationsApi.refundTransfer(tenantId, transactionId, requestBody, response);

        // Assert
        assertEquals("{\"response\":\"Requested incoming transfer does not exist or not yet completed!\"}", result);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Retrieve transfer details successfully with valid workflowInstanceKey")
    @Test
    void test_transfer_details_success() {
        Long workflowInstanceKey = 123L;
        Transfer transfer = new Transfer(workflowInstanceKey);
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task());
        tasks.add(new Task());
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable());
        variables.add(new Variable());

        when(transferRepository.findFirstByWorkflowInstanceKey(workflowInstanceKey)).thenReturn(transfer);
        when(taskRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(tasks);
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(variables);

        TransferDetail result = operationsApi.transferDetails(workflowInstanceKey);

        assertNotNull(result);
        assertEquals(transfer, result.getTransfer());
        assertEquals(tasks, result.getTasks());
        assertEquals(variables, result.getVariables());
    }

    @DisplayName("Handle non-existent workflowInstanceKey gracefully")
    @Test
    void test_transfer_details_non_existent_key() {
        Long workflowInstanceKey = 999L;

        when(transferRepository.findFirstByWorkflowInstanceKey(workflowInstanceKey)).thenReturn(null);
        when(taskRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(new ArrayList<>());
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(new ArrayList<>());

        TransferDetail result = operationsApi.transferDetails(workflowInstanceKey);

        assertNotNull(result);
        assertNull(result.getTransfer());
        assertTrue(result.getTasks().isEmpty());
        assertTrue(result.getVariables().isEmpty());
    }

    @DisplayName("Returns TransactionRequestDetail for valid workflowInstanceKey")
    @Test
    void test_transaction_request_details_valid_key() {
        Long workflowInstanceKey = 123L;
        TransactionRequest transactionRequest = new TransactionRequest();
        List<Task> tasks = new ArrayList<>();
        List<Variable> variables = new ArrayList<>();

        when(transactionRequestRepository.findFirstByWorkflowInstanceKey(workflowInstanceKey)).thenReturn(transactionRequest);
        when(taskRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(tasks);
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(variables);


        TransactionRequestDetail result = operationsApi.transactionRequestDetails(workflowInstanceKey);

        assertNotNull(result);
        assertEquals(transactionRequest, result.getTransactionRequest());
        assertEquals(tasks, result.getTasks());
        assertEquals(variables, result.getVariables());
    }

    @DisplayName("workflowInstanceKey does not exist in TransactionRequestRepository")
    @Test
    void test_transaction_request_details_invalid_key() {
        Long workflowInstanceKey = 999L;


        when(transactionRequestRepository.findFirstByWorkflowInstanceKey(workflowInstanceKey)).thenReturn(null);
        when(taskRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(new ArrayList<>());
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(workflowInstanceKey)).thenReturn(new ArrayList<>());

        TransactionRequestDetail result = operationsApi.transactionRequestDetails(workflowInstanceKey);

        assertNotNull(result);
        assertNull(result.getTransactionRequest());
        assertTrue(result.getTasks().isEmpty());
        assertTrue(result.getVariables().isEmpty());
    }

    @DisplayName("Returns a list of lists of variables for valid businessKey and businessKeyType")
    @Test
    void test_returns_list_of_lists_of_variables_for_valid_businessKey_and_businessKeyType() {
        // Arrange
        String businessKey = "validKey";
        String businessKeyType = "validType";
        BusinessKey businessKey1 = new BusinessKey();
        businessKey1.setWorkflowInstanceKey(1L);
        BusinessKey businessKey2 = new BusinessKey();
        businessKey2.setWorkflowInstanceKey(2L);
        List<BusinessKey> businessKeys = Arrays.asList(businessKey1, businessKey2);

        Variable variable1 = new Variable();
        variable1.setName("var1");
        Variable variable2 = new Variable();
        variable2.setName("var2");
        List<Variable> variables1 = Arrays.asList(variable1);
        List<Variable> variables2 = Arrays.asList(variable2);

        when(businessKeyRepository.findByBusinessKeyAndBusinessKeyType(businessKey, businessKeyType)).thenReturn(businessKeys);
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(1L)).thenReturn(variables1);
        when(variableRepository.findByWorkflowInstanceKeyOrderByTimestamp(2L)).thenReturn(variables2);

        // Act
        List<List<Variable>> result = operationsApi.variables(businessKey, businessKeyType);

        // Assert
        assertEquals(2, result.size());
        assertEquals(variables1, result.get(0));
        assertEquals(variables2, result.get(1));
    }

    @DisplayName("Handles empty list when no business keys are found")
    @Test
    void test_handles_empty_list_when_no_business_keys_are_found() {
        // Arrange
        String businessKey = "invalidKey";
        String businessKeyType = "invalidType";

        when(businessKeyRepository.findByBusinessKeyAndBusinessKeyType(businessKey, businessKeyType)).thenReturn(Collections.emptyList());

        // Act
        List<List<Variable>> result = operationsApi.variables(businessKey, businessKeyType);

        // Assert
        assertTrue(result.isEmpty());
    }

    @DisplayName("Retrieve tasks successfully for valid businessKey and businessKeyType")
    @Test
    void test_retrieve_tasks_successfully() {
        // Arrange
        String businessKey = "validBusinessKey";
        String businessKeyType = "validBusinessKeyType";
        BusinessKey businessKey1 = new BusinessKey();
        businessKey1.setWorkflowInstanceKey(1L);
        List<BusinessKey> businessKeys = Arrays.asList(businessKey1);
        Task task = new Task();
        List<Task> tasks = Arrays.asList(task);

        when(businessKeyRepository.findByBusinessKeyAndBusinessKeyType(businessKey, businessKeyType)).thenReturn(businessKeys);
        when(taskRepository.findByWorkflowInstanceKeyOrderByTimestamp(1L)).thenReturn(tasks);

        // Act
        List<List<Task>> result = operationsApi.tasks(businessKey, businessKeyType);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(tasks, result.get(0));
    }

    @DisplayName("Handle null or empty businessKey parameter")
    @Test
    void test_handle_null_or_empty_business_key() {
        // Arrange
        String businessKey = "";
        String businessKeyType = "validBusinessKeyType";

        when(businessKeyRepository.findByBusinessKeyAndBusinessKeyType(businessKey, businessKeyType)).thenReturn(Collections.emptyList());

        // Act
        List<List<Task>> result = operationsApi.tasks(businessKey, businessKeyType);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}