package org.apache.fineract.operations;

public enum TransactionRequestState {

    IN_PROGRESS,
    RECEIVED,
    ACCEPTED,
    REJECTED,
    NOT_AUTOSAVED,
    FAILED;
}