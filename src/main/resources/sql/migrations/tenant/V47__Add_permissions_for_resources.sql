update m_permission
set code = 'READ_TRANSFER', `grouping` = 'transfers', entity_name = 'TRANSFER', action_name = 'READ'
where code = 'REFUND' and `grouping` = 'operations';

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('CREATE_TRANSFER', 'transfers', 'TRANSFER', 'CREATE', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('EXPORT_TRANSFER', 'transfers', 'TRANSFER', 'EXPORT', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('READ_USER', 'authorisation', 'USER', 'READ', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('CREATE_USER', 'authorisation', 'USER', 'CREATE', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('UPDATE_USER', 'authorisation', 'USER', 'UPDATE', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('SUSPEND_USER', 'authorisation', 'USER', 'SUSPEND', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('ACTIVATE_USER', 'authorisation', 'USER', 'ACTIVATE', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('DELETE_USER', 'authorisation', 'USER', 'DELETE', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('READ_AUDIT', 'audit', 'AUDIT', 'READ', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('EXPORT_TRANSACTION_REQUEST', 'transactionRequests', 'TRANSACTION_REQUEST', 'EXPORT', 0);

insert into m_permission (code, `grouping`, entity_name, action_name, can_maker_checker)
values ('READ_TRANSACTION_REQUEST', 'transactionRequests', 'TRANSACTION_REQUEST', 'READ', 0);
