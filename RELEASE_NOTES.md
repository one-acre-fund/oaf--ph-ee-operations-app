Release Notes

## Version 1.1.13 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * User Management
            * [CP-4069] - Add API to fetch user details by username
            * [CP-4069] - Improvement on update user and role

## Version 1.1.12 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * User Management
            * [CP-3677] - Implement user filtering for roles and status

## Version 1.1.11 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * User Management
            * [CP-3675] - Implement user downloads with roles

## Version 1.1.10 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * User Management
            * [CP-3681] - Implement permissions for the PH Ops App backend

## Version 1.1.9 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        *  Elastic APM + OpenTelemetry Integration
            * [CP-3980] - Integrate Elastic APM with PH

## Version 1.1.8 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * User Management
            * [CP-3958] - Automatically create new Keycloak users

## Version 1.1.7 - Up to date with Mifos Version 1.5.0
    * Payment Hub OPs
        * Authentication and Security
            * [CP-3662] - Add  Keycloak integration for authentication and authorization
        * Miscellaneous
            * [CP-3757] - Switch CI build to GitHub Actions
        * Multi-tenancy
            * [CP-3931] - Database check/creation per tenant during application startup

## Version 1.1.6 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [CP-3399] - Fix issue with payments incomming transactions not being returned

## Version 1.1.5 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [SER-3003] - Rename Payer DFSP Id to ‘AMS Business Short Code’ 

## Version 1.1.4 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Audits
            * [SER-3117] - Add audit fields on variables table

## Version 1.1.3 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Audits
            * [SER-3099] - Add audit fields on the transaction response

## Version 1.1.2 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Audits
            * [SER-2070] - Setup audit of user actions + add jpa auditing

## Version 1.1.1 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [SER-2990] - Add NOT_AUTOSAVED status for transaction requests

## Version 1.1.0 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transfers
            * [SER-2925] - Create API for exporting transfers

## Version 1.0.2 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [SER-1596] - Filter incoming transactions by the MNO reference/externalId
            * [SER-2208] - Fix bugs in searching incoming transactions in PaymentHub Service.

## Version 1.0.1 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [SER-1926] - Ensure the exported csv contains accurate data

## Version 1.0.0 - Up to date with Mifos Version 1.5.0

    * Payment Hub OPs
        * Transaction Requests
            * [SER-1605] - Filter incoming transactions by user's currency and payeePartyIds
            * [SER-1646] - Filter incoming transactions by user's assigned payeePartyIdTypes
            * [SER-1926] - Ensure the exported csv contains accurate data
            * [SER-1596] - Filter incoming transactions by the MNO reference/externalId
            * [SER-2208] - Fix bugs in searching incoming transactions in PaymentHub Service.
        * Deployment
            * [SER-1875] - Add pipeline deployment file for Azure
        * Audit
            * [SER-1822] - Setup audit of user actions
        * User Management
            * [SER-1656] - Activate and deactivate users
