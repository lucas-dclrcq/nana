create sequence fastDownloadQuota_SEQ start with 1 increment by 50;

create table fastDownloadQuota
(
    id        bigint  not null,
    remaining integer not null,
    total     integer not null,
    updatedAt timestamp(6) with time zone not null,
    primary key (id)
);
