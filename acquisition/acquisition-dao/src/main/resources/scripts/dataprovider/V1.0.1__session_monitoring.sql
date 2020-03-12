-- Track session owner and session in scan files
alter table t_acquisition_file ADD session_owner varchar(64) not null;
alter table t_acquisition_file ADD session varchar(128) not null;


