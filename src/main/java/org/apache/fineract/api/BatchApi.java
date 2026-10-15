package org.apache.fineract.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.fineract.config.PaymentModeConfiguration;
import org.apache.fineract.exception.BatchResultFileException;
import org.apache.fineract.file.FileTransferService;
import org.apache.fineract.operations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.web.bind.annotation.*;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.apache.fineract.core.service.OperatorUtils.strip;

@RestController
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/api/v1")
public class BatchApi {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Autowired
    private PaymentModeConfiguration paymentModeConfig;

    @Value("${application.bucket-name}")
    private String bucketName;

    @GetMapping("/batches")
    public Page<Batch> getBatches(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                  @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
                                  @RequestParam(value = "sortedBy", required = false) String sortedBy,
                                  @RequestParam(value = "sortedOrder", required = false, defaultValue = "DESC") String sortedOrder
    ) {
        Specifications<Batch> specifications = BatchSpecs.match(Batch_.subBatchId, null);

        PageRequest pager;
        if (sortedBy == null || "startedAt".equals(sortedBy)) {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), "startedAt"));
        } else {
            pager = new PageRequest(page, size, new Sort(Sort.Direction.fromString(sortedOrder), sortedBy));
        }

        return batchRepository.findAll(specifications, pager);
    }

    @GetMapping("/batch")
    public BatchDTO batchDetails(@RequestParam(value = "batchId", required = false) String batchId,
                                 @RequestParam(value = "requestId", required = false) String requestId) {
        Batch batch = batchRepository.findByBatchId(batchId);
        if (batch != null) {
            if (batch.getResultGeneratedAt() != null) {
//                Checks if last status was checked before 10 mins
                if (new Date().getTime() - batch.getResultGeneratedAt().getTime() < 600000) {
                    return generateDetails(batch);
                } else {
                    return generateDetails(batch);
                }
            } else {
                return generateDetails(batch);
            }
        } else {
           Batch batch1 = new Batch();
           batch1.setBatchId(batchId);
           batch1.setRequestId(requestId);
           return generateDetails(batch1);
        }

    }

    @GetMapping("/batch/detail")
    public Page<Transfer> batchDetails(HttpServletResponse httpServletResponse,
                                                       @RequestParam(value = "batchId") String batchId,
                                                       @RequestParam(value = "status", defaultValue = "ALL") String status,
                                                       @RequestParam(value = "pageNo", defaultValue = "0") Integer pageNo,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(value = "command", required = false, defaultValue = "json") String command) {

        if (command.equalsIgnoreCase("download")) {
            Batch batch = batchRepository.findByBatchId(batchId);
            if (batch != null && batch.getResult_file() != null) {
                httpServletResponse.setHeader("Location", batch.getResult_file());
                httpServletResponse.setStatus(302);
            } else {
                httpServletResponse.setStatus(404);
            }
            return null;
        }

        Page<Transfer> transfers;

        if (status.equalsIgnoreCase(TransferStatus.COMPLETED.toString()) ||
                status.equalsIgnoreCase(TransferStatus.IN_PROGRESS.toString()) ||
                status.equalsIgnoreCase(TransferStatus.FAILED.toString())) {
            transfers = transferRepository.findAllByBatchIdAndStatus(batchId, status.toUpperCase(), new PageRequest(pageNo, pageSize));
        } else {
            transfers = transferRepository.findAllByBatchId(batchId, new PageRequest(pageNo, pageSize));
        }

        return transfers;
    }

    @GetMapping("/batch/transactions")
    public HashMap<String, String> batchTransactionDetails(@RequestParam String batchId) {
        Batch batch = batchRepository.findByBatchId(batchId);
        if (batch != null) {
            List<Transfer> transfers = transferRepository.findAllByBatchId(batch.getBatchId());
            HashMap<String, String> status = new HashMap<>();
            for(Transfer transfer: transfers){
                status.put(transfer.getTransactionId(), transfer.getStatus().name());
            }
            return status;
        } else {
            return null;
        }
    }

    private BatchDTO generateDetails (Batch batch) {

        StringBuilder modes = new StringBuilder();

        List<Transfer> transfers = transferRepository.findAllByBatchId(batch.getBatchId());

        List<Batch> allBatches = batchRepository.findAllByBatchId(batch.getBatchId());

        Long completed = 0L;
        Long failed = 0L;
        Long total = 0L;
        Long ongoing = 0L;
        Double batchFailedPercent = 0.0;
        Double batchCompletedPercent = 0.0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal completedAmount = BigDecimal.ZERO;
        BigDecimal ongoingAmount = BigDecimal.ZERO;
        BigDecimal failedAmount = BigDecimal.ZERO;

        for (Transfer transfer : transfers) {
            Optional<Variable> variable = variableRepository.findByWorkflowInstanceKeyAndZeebeGenerationAndVariableName(
                    "paymentMode",
                    transfer.getWorkflowInstanceKey(),
                    transfer.getZeebeGeneration());
            if (variable.isPresent()) {
                // this will prevent 2x count of variables by eliminating data from transfers table
                if (paymentModeConfig.getByMode(strip(variable.get().getValue()))
                        .getType().equalsIgnoreCase("BATCH")) {
                    continue;
                }
            }
            total++;
            BigDecimal amount = transfer.getAmount();
            totalAmount = totalAmount.add(amount);
            if (transfer.getStatus().equals(TransferStatus.COMPLETED)) {
                completed++;
                completedAmount = completedAmount.add(amount);
            } else if (transfer.getStatus().equals(TransferStatus.FAILED)) {
                failed++;
                failedAmount = failedAmount.add(amount);
            } else if (transfer.getStatus().equals(TransferStatus.IN_PROGRESS)) {
                ongoing++;
                ongoingAmount = ongoingAmount.add(amount);
            }
        }

        // calculating matrices for sub batches
        Long subBatchFailed = 0L;
        Long subBatchCompleted = 0L;
        Long subBatchOngoing = 0L;
        Long subBatchTotal = 0L;


        for (Batch bt: allBatches) {
            if (bt.getPaymentMode() != null && !modes.toString().contains(bt.getPaymentMode())) {
                if (!modes.toString().equals("")) {
                    modes.append(",");
                }
                modes.append(bt.getPaymentMode());
            }
            if (bt.getSubBatchId() == null || bt.getSubBatchId().isEmpty()) {
                continue;
            }
            if (bt.getFailed() != null) {
                subBatchFailed += bt.getFailed();
                failedAmount = failedAmount.add(BigDecimal.valueOf(bt.getFailedAmount()));
            }
            if (bt.getCompleted() != null) {
                subBatchCompleted += bt.getCompleted();
                completedAmount = completedAmount.add(BigDecimal.valueOf(bt.getCompletedAmount()));
            }
            if (bt.getOngoing() != null) {
                subBatchOngoing += bt.getOngoing();
                ongoingAmount = ongoingAmount.add(BigDecimal.valueOf(bt.getOngoingAmount()));
            }
            if (bt.getTotalTransactions() != null) {
                subBatchTotal += bt.getTotalTransactions();
                totalAmount = totalAmount.add(BigDecimal.valueOf(bt.getTotalAmount()));
            }
        }

        // updating the data with sub batches details
        completed += subBatchCompleted;
        failed += subBatchFailed;
        total += subBatchTotal;


        ongoing += subBatchOngoing;

        if (batch.getResult_file() == null || (batch.getResult_file() != null && batch.getResult_file().isEmpty())) {
            batch.setResult_file(createDetailsFile(batch.getBatchId(), transfers));
        }
        batch.setCompleted(completed);
        batch.setFailed(failed);
        batch.setResultGeneratedAt(new Date());
        batch.setOngoing(ongoing);
        batch.setTotalTransactions(total);
        batchRepository.save(batch);
        batchCompletedPercent = (double) batch.getCompleted() / total * 100;
        batchFailedPercent = (double) batch.getFailed() / total * 100;

        BatchDTO response = new BatchDTO(batch.getBatchId(),
                batch.getRequestId(), batch.getTotalTransactions(), batch.getOngoing(),
                batch.getFailed(), batch.getCompleted(), totalAmount, completedAmount,
                ongoingAmount, failedAmount, batch.getResult_file(), batch.getNote(),
                batchCompletedPercent.toString(), batchFailedPercent.toString());

        response.setCreated_at(""+batch.getStartedAt());
        response.setModes(modes.toString());
        response.setPurpose("Unknown purpose");
       System.out.println("Batch details generated for batchId: " + response.getSuccessPercentage());

        if (batch.getCompleted().longValue() == batch.getTotalTransactions().longValue()) {
            response.setStatus("COMPLETED");
        } else if (batch.getOngoing() != 0 && batch.getCompletedAt() == null) {
            response.setStatus("Pending");
        } else if (batch.getFailed().longValue() == batch.getFailed().longValue()) {
            response.setStatus("Failed");
        } else {
            response.setStatus("UNKNOWN");
        }

        return response;
    }

    private String createDetailsFile(String batchId, List<Transfer> transfers) {
        File tempFile = null;
        try {
            tempFile = Files.createTempFile("batch_result_", ".csv").toFile();
            try (ICsvListWriter csvWriter = new CsvListWriter(
                    new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8),
                    CsvPreference.STANDARD_PREFERENCE)) {
                for (Transfer transfer : transfers) {
                    csvWriter.write(
                            transfer.getTransactionId(),
                            transfer.getStatus(),
                            transfer.getPayeeDfspId(),
                            transfer.getPayeePartyId(),
                            transfer.getAmsBusinessShortCode(),
                            transfer.getPayerPartyId(),
                            transfer.getAmount(),
                            transfer.getCurrency(),
                            transfer.getErrorInformation(),
                            formatDate(transfer.getStartedAt()),
                            formatDate(transfer.getCompletedAt()));
                }
            }
            return fileTransferService.uploadFile(tempFile, bucketName);
        } catch (IOException | RuntimeException e) {
            throw new BatchResultFileException("Failed to create the result file for batch " + batchId, e);
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                logger.warn("Could not delete temp file");
            }
        }
    }

    private static String formatDate(Date date) {
        return date == null ? null : DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(date.getTime()));
    }

}
