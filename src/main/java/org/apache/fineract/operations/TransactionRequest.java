package org.apache.fineract.operations;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.apache.fineract.operations.TransactionRequestState.IN_PROGRESS;


@Entity
@Table(name = "transaction_requests")
public class TransactionRequest extends AbstractPersistableCustom<Long> {

    @Column(name = "WORKFLOW_INSTANCE_KEY")
    private String workflowInstanceKey;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;

    @Column(name = "STARTED_AT")
    private Date startedAt;

    @Column(name = "COMPLETED_AT")
    private Date completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE")
    private TransactionRequestState state;

    @Column(name = "PAYEE_DFSP_ID")
    private String payeeDfspId;
    @Column(name = "PAYEE_PARTY_ID")
    private String payeePartyId;
    @Column(name = "PAYEE_PARTY_ID_TYPE")
    private String payeePartyIdType;
    @Column(name = "PAYEE_FEE")
    private BigDecimal payeeFee;
    @Column(name = "PAYEE_QUOTE_CODE")
    private String payeeQuoteCode;

    @Column(name = "AMS_BUSINESS_SHORT_CODE")
    private String amsBusinessShortCode;
    @Column(name = "PAYER_PARTY_ID")
    private String payerPartyId;
    @Column(name = "PAYER_PARTY_ID_TYPE")
    private String payerPartyIdType;
    @Column(name = "PAYER_FEE")
    private BigDecimal payerFee;
    @Column(name = "PAYER_QUOTE_CODE")
    private String payerQuoteCode;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "CURRENCY")
    private String currency;

    @Column(name = "DIRECTION")
    private String direction;

    @Column(name = "AUTH_TYPE")
    private String authType;

    @Column(name = "INITIATOR_TYPE")
    private String initiatorType;

    @Column(name = "SCENARIO")
    private String scenario;

    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "CLIENTCORRELATIONID")
    private String clientCorrelationId;

    @Column(name = "ERROR_INFORMATION")
    private String errorInformation;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "transactionRequest", fetch = FetchType.LAZY)
    private List<Variable> variables;

    @JsonIgnore
    @JsonManagedReference
    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public TransactionRequest() {
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public TransactionRequest(String workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
        this.state = IN_PROGRESS;
    }

    public String getWorkflowInstanceKey() {
        return workflowInstanceKey;
    }

    public void setWorkflowInstanceKey(String workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public TransactionRequestState getState() {
        return state;
    }

    public void setState(TransactionRequestState state) {
        this.state = state;
    }

    public String getPayeeDfspId() {
        return payeeDfspId;
    }

    public void setPayeeDfspId(String payeeDfspId) {
        this.payeeDfspId = payeeDfspId;
    }

    public String getPayeePartyId() {
        return payeePartyId;
    }

    public void setPayeePartyId(String payeePartyId) {
        this.payeePartyId = payeePartyId;
    }

    public String getPayeePartyIdType() {
        return payeePartyIdType;
    }

    public void setPayeePartyIdType(String payeePartyIdType) {
        this.payeePartyIdType = payeePartyIdType;
    }

    public BigDecimal getPayeeFee() {
        return payeeFee;
    }

    public void setPayeeFee(BigDecimal payeeFee) {
        this.payeeFee = payeeFee;
    }

    public String getPayeeQuoteCode() {
        return payeeQuoteCode;
    }

    public void setPayeeQuoteCode(String payeeQuoteCode) {
        this.payeeQuoteCode = payeeQuoteCode;
    }

    public String getAmsBusinessShortCode() {
        return amsBusinessShortCode;
    }

    public void setAmsBusinessShortCode(String amsBusinessShortCode) {
        this.amsBusinessShortCode = amsBusinessShortCode;
    }

    public String getPayerPartyId() {
        return payerPartyId;
    }

    public void setPayerPartyId(String payerPartyId) {
        this.payerPartyId = payerPartyId;
    }

    public String getPayerPartyIdType() {
        return payerPartyIdType;
    }

    public void setPayerPartyIdType(String payerPartyIdType) {
        this.payerPartyIdType = payerPartyIdType;
    }

    public BigDecimal getPayerFee() {
        return payerFee;
    }

    public void setPayerFee(BigDecimal payerFee) {
        this.payerFee = payerFee;
    }

    public String getPayerQuoteCode() {
        return payerQuoteCode;
    }

    public void setPayerQuoteCode(String payerQuoteCode) {
        this.payerQuoteCode = payerQuoteCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getInitiatorType() {
        return initiatorType;
    }

    public void setInitiatorType(String initiatorType) {
        this.initiatorType = initiatorType;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getClientCorrelationId() {
        return clientCorrelationId;
    }

    public void setClientCorrelationId(String clientCorrelationId) {
        this.clientCorrelationId = clientCorrelationId;
    }

    public String getErrorInformation() {
        return errorInformation;
    }

    public void setErrorInformation(String errorInformation) {
        this.errorInformation = errorInformation;
    }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "workflowInstanceKey='" + workflowInstanceKey + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", state=" + state +
                ", payeeDfspId='" + payeeDfspId + '\'' +
                ", payeePartyId='" + payeePartyId + '\'' +
                ", payeePartyIdType='" + payeePartyIdType + '\'' +
                ", payeeFee=" + payeeFee +
                ", payeeQuoteCode='" + payeeQuoteCode + '\'' +
                ", amsBusinessShortCode='" + amsBusinessShortCode + '\'' +
                ", payerPartyId='" + payerPartyId + '\'' +
                ", payerPartyIdType='" + payerPartyIdType + '\'' +
                ", payerFee=" + payerFee +
                ", payerQuoteCode='" + payerQuoteCode + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", direction='" + direction + '\'' +
                ", authType='" + authType + '\'' +
                ", initiatorType='" + initiatorType + '\'' +
                ", scenario='" + scenario + '\'' +
                ", externalId='" + externalId + '\'' +
                ", clientCorrelationId='" + clientCorrelationId + '\'' +
                ", errorInformation='" + errorInformation + '\'' +
                ", variables=" + variables +
                '}';
    }
}
