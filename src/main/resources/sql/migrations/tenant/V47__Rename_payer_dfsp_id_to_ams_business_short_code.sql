ALTER TABLE transfers
    RENAME COLUMN PAYER_DFSP_ID TO AMS_BUSINESS_SHORT_CODE;
ALTER TABLE transaction_requests
    RENAME COLUMN PAYER_DFSP_ID TO AMS_BUSINESS_SHORT_CODE;