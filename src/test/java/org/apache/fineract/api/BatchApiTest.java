package org.apache.fineract.api;

import org.apache.fineract.exception.BatchResultFileException;
import org.apache.fineract.file.FileTransferService;
import org.apache.fineract.operations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BatchApiTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private FileTransferService fileTransferService;

    @InjectMocks
    private BatchApi batchApi;

    private Batch batch;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batch = new Batch();
        batch.setBatchId("batch-1");
        when(batchRepository.findByBatchId("batch-1")).thenReturn(batch);
        when(batchRepository.findAllByBatchId("batch-1")).thenReturn(Collections.singletonList(batch));
    }

    @Test
    @DisplayName("Result file is valid CSV, handles missing dates and is deleted after upload")
    void resultFileIsEscapedAndNullSafe() {
        Transfer completed = transfer("tx-1", TransferStatus.COMPLETED, null);
        completed.setStartedAt(new Date(0));
        completed.setCompletedAt(new Date(1000));
        Transfer inProgress = transfer("tx-2", TransferStatus.IN_PROGRESS, "{\"msg\": \"timeout, \"retry\"\nlater\"}");
        when(transferRepository.findAllByBatchId("batch-1")).thenReturn(Arrays.asList(completed, inProgress));

        AtomicReference<File> uploaded = new AtomicReference<>();
        AtomicReference<String> content = new AtomicReference<>();
        when(fileTransferService.uploadFile(any(File.class), any())).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            uploaded.set(file);
            content.set(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return "https://bucket/result.csv";
        });

        BatchDTO response = batchApi.batchDetails("batch-1", null);

        assertEquals("https://bucket/result.csv", response.getFile());
        assertEquals(
                "tx-1,COMPLETED,payee-dfsp,payee-1,ams-1,payer-1,10.50,KES,,1970-01-01T00:00:00Z,1970-01-01T00:00:01Z\r\n"
                        + "tx-2,IN_PROGRESS,payee-dfsp,payee-1,ams-1,payer-1,10.50,KES,"
                        + "\"{\"\"msg\"\": \"\"timeout, \"\"retry\"\"\r\nlater\"\"}\",,\r\n",
                content.get());
        assertFalse(uploaded.get().exists(), "temp file should be deleted");
    }

    @Test
    @DisplayName("Upload failure is rethrown and the temp file is still deleted")
    void uploadFailureIsNotSwallowed() {
        when(transferRepository.findAllByBatchId("batch-1"))
                .thenReturn(Collections.singletonList(transfer("tx-1", TransferStatus.COMPLETED, null)));
        AtomicReference<File> uploaded = new AtomicReference<>();
        when(fileTransferService.uploadFile(any(File.class), any())).thenAnswer(invocation -> {
            uploaded.set(invocation.getArgument(0));
            throw new IllegalStateException("S3 unavailable");
        });

        BatchResultFileException exception = assertThrows(BatchResultFileException.class,
                () -> batchApi.batchDetails("batch-1", null));

        assertTrue(exception.getMessage().contains("batch-1"));
        assertFalse(uploaded.get().exists(), "temp file should be deleted");
        verify(batchRepository, never()).save(any());
    }

    private Transfer transfer(String transactionId, TransferStatus status, String errorInformation) {
        Transfer transfer = new Transfer();
        transfer.setTransactionId(transactionId);
        transfer.setStatus(status);
        transfer.setPayeeDfspId("payee-dfsp");
        transfer.setPayeePartyId("payee-1");
        transfer.setAmsBusinessShortCode("ams-1");
        transfer.setPayerPartyId("payer-1");
        transfer.setAmount(new BigDecimal("10.50"));
        transfer.setCurrency("KES");
        transfer.setErrorInformation(errorInformation);
        return transfer;
    }
}
