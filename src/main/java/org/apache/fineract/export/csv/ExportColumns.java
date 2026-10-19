package org.apache.fineract.export.csv;

import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.Transfer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.fineract.export.csv.ExportColumn.of;

/**
 * Columns of each export, in order. Headers and order match the legacy exports (entity field names in upper case),
 * so existing consumers of the files keep working.
 */
public final class ExportColumns {

    public static final List<ExportColumn<Transfer>> TRANSFERS = Collections.unmodifiableList(Arrays.asList(
            of("WORKFLOWINSTANCEKEY", Transfer::getWorkflowInstanceKey),
            of("ZEEBEGENERATION", Transfer::getZeebeGeneration),
            of("TRANSACTIONID", Transfer::getTransactionId),
            of("STARTEDAT", Transfer::getStartedAt),
            of("COMPLETEDAT", Transfer::getCompletedAt),
            of("STATUS", Transfer::getStatus),
            of("STATUSDETAIL", Transfer::getStatusDetail),
            of("PAYEEDFSPID", Transfer::getPayeeDfspId),
            of("PAYEEPARTYID", Transfer::getPayeePartyId),
            of("PAYEEPARTYIDTYPE", Transfer::getPayeePartyIdType),
            of("PAYEEFEE", Transfer::getPayeeFee),
            of("PAYEEFEECURRENCY", Transfer::getPayeeFeeCurrency),
            of("PAYEEQUOTECODE", Transfer::getPayeeQuoteCode),
            of("AMSBUSINESSSHORTCODE", Transfer::getAmsBusinessShortCode),
            of("PAYERPARTYID", Transfer::getPayerPartyId),
            of("PAYERPARTYIDTYPE", Transfer::getPayerPartyIdType),
            of("PAYERFEE", Transfer::getPayerFee),
            of("PAYERFEECURRENCY", Transfer::getPayerFeeCurrency),
            of("PAYERQUOTECODE", Transfer::getPayerQuoteCode),
            of("AMOUNT", Transfer::getAmount),
            of("CURRENCY", Transfer::getCurrency),
            of("DIRECTION", Transfer::getDirection),
            of("ERRORINFORMATION", Transfer::getErrorInformation),
            of("BATCHID", Transfer::getBatchId),
            of("CLIENTCORRELATIONID", Transfer::getClientCorrelationId)));

    public static final List<ExportColumn<TransactionRequest>> TRANSACTION_REQUESTS = Collections.unmodifiableList(
            Arrays.asList(
                    of("WORKFLOWINSTANCEKEY", TransactionRequest::getWorkflowInstanceKey),
                    of("ZEEBEGENERATION", TransactionRequest::getZeebeGeneration),
                    of("TRANSACTIONID", TransactionRequest::getTransactionId),
                    of("STARTEDAT", TransactionRequest::getStartedAt),
                    of("COMPLETEDAT", TransactionRequest::getCompletedAt),
                    of("STATE", TransactionRequest::getState),
                    of("PAYEEDFSPID", TransactionRequest::getPayeeDfspId),
                    of("PAYEEPARTYID", TransactionRequest::getPayeePartyId),
                    of("PAYEEPARTYIDTYPE", TransactionRequest::getPayeePartyIdType),
                    of("PAYEEFEE", TransactionRequest::getPayeeFee),
                    of("PAYEEQUOTECODE", TransactionRequest::getPayeeQuoteCode),
                    of("AMSBUSINESSSHORTCODE", TransactionRequest::getAmsBusinessShortCode),
                    of("PAYERPARTYID", TransactionRequest::getPayerPartyId),
                    of("PAYERPARTYIDTYPE", TransactionRequest::getPayerPartyIdType),
                    of("PAYERFEE", TransactionRequest::getPayerFee),
                    of("PAYERQUOTECODE", TransactionRequest::getPayerQuoteCode),
                    of("AMOUNT", TransactionRequest::getAmount),
                    of("CURRENCY", TransactionRequest::getCurrency),
                    of("DIRECTION", TransactionRequest::getDirection),
                    of("AUTHTYPE", TransactionRequest::getAuthType),
                    of("INITIATORTYPE", TransactionRequest::getInitiatorType),
                    of("SCENARIO", TransactionRequest::getScenario),
                    of("EXTERNALID", TransactionRequest::getExternalId),
                    of("CLIENTCORRELATIONID", TransactionRequest::getClientCorrelationId),
                    of("ERRORINFORMATION", TransactionRequest::getErrorInformation)));

    private ExportColumns() {
    }
}
