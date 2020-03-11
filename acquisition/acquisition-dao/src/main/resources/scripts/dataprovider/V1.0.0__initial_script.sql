create table public.t_acq_file_info (id int8 not null, comment text, data_type varchar(16), lastModificationDate timestamp, mandatory boolean, mime_type varchar(255), scan_conf_id int8 not null, acq_chain_id int8, primary key (id));
create table public.t_acq_processing_chain (id int8 not null, active boolean, categories jsonb, ingest_chain varchar(255), label varchar(64) not null, last_activation_date timestamp, locked boolean not null, mode varchar(16), period varchar(255), storages jsonb, generatesip_conf_id int8 not null, acq_job_info_id uuid, postprocesssip_conf_id int8, product_conf_id int8 not null, validation_conf_id int8 not null, primary key (id));
create table public.t_acquisition_file (id int8 not null, acquisition_date timestamp, error text, filePath varchar(255), state varchar(32), acq_file_info_id int8, product_id int8, primary key (id));
create table public.t_acquisition_product (id int8 not null, error text, ip_id varchar(128), last_update timestamp not null, product_name varchar(128), session varchar(128), json_sip jsonb, sip_state varchar(32) not null, product_state varchar(32) not null, post_prod_job_info_id uuid, sip_gen_job_info_id uuid, processing_chain_id int8, primary key (id));
create index idx_acq_file_state on public.t_acquisition_file (state);
create index idx_acq_file_state_file_info on public.t_acquisition_file (state, acq_file_info_id);
create index idx_acq_file_info on public.t_acquisition_file (acq_file_info_id);
create index idx_acq_processing_chain on public.t_acquisition_product (processing_chain_id);
create index idx_acq_product_name on public.t_acquisition_product (product_name);
create index idx_acq_product_sip_state on public.t_acquisition_product (sip_state);
create index idx_acq_product_state on public.t_acquisition_product (product_state);
alter table public.t_acquisition_product add constraint uk_acq_product_ipId unique (ip_id);
alter table public.t_acquisition_product add constraint uk_acq_product_name unique (product_name);
create sequence public.seq_acq_chain start 1 increment 50;
create sequence public.seq_acq_file start 1 increment 50;
create sequence public.seq_acq_file_info start 1 increment 50;
create sequence public.seq_product start 1 increment 50;
alter table public.t_acq_file_info add constraint fk_scan_conf_id foreign key (scan_conf_id) references public.t_plugin_configuration;
alter table public.t_acq_file_info add constraint fk_acq_chain_id foreign key (acq_chain_id) references public.t_acq_processing_chain;
alter table public.t_acq_processing_chain add constraint fk_generatesip_conf_id foreign key (generatesip_conf_id) references public.t_plugin_configuration;
alter table public.t_acq_processing_chain add constraint fk_acq_job_info_id foreign key (acq_job_info_id) references public.t_job_info;
alter table public.t_acq_processing_chain add constraint fk_postprocesssip_conf_id foreign key (postprocesssip_conf_id) references public.t_plugin_configuration;
alter table public.t_acq_processing_chain add constraint fk_product_conf_id foreign key (product_conf_id) references public.t_plugin_configuration;
alter table public.t_acq_processing_chain add constraint fk_validation_conf_id foreign key (validation_conf_id) references public.t_plugin_configuration;
alter table public.t_acquisition_file add constraint fk_acq_file_info_id foreign key (acq_file_info_id) references public.t_acq_file_info;
alter table public.t_acquisition_file add constraint fk_product_id foreign key (product_id) references public.t_acquisition_product;
alter table public.t_acquisition_product add constraint fk_post_prod_job_info_id foreign key (post_prod_job_info_id) references public.t_job_info;
alter table public.t_acquisition_product add constraint fk_sip_gen_job_info_id foreign key (sip_gen_job_info_id) references public.t_job_info;
alter table public.t_acquisition_product add constraint fk_processing_chain_id foreign key (processing_chain_id) references public.t_acq_processing_chain;
