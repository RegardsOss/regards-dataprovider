alter table t_acq_processing_chain drop column period;
alter table t_acq_processing_chain add column period text;
alter table t_acq_processing_chain add column storages jsonb;
alter table t_acq_processing_chain add column categories jsonb;
alter table t_acq_processing_chain drop column session;

alter table t_acquisition_file drop column checksum;
alter table t_acquisition_file drop column checksumalgorithm;
