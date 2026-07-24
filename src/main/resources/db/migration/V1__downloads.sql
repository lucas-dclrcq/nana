create sequence download_SEQ start with 1 increment by 50;

create table download
(
    id              bigint        not null,
    md5             varchar(32)   not null,
    title           varchar(1000) not null,
    author          varchar(500),
    extension       varchar(20),
    requestedBy     varchar(255)  not null,
    status          varchar(20)   not null check ((status in ('PENDING', 'DOWNLOADING', 'SUCCESS', 'FAILED'))),
    requestedAt     timestamp(6) with time zone not null,
    startedAt       timestamp(6) with time zone,
    finishedAt      timestamp(6) with time zone,
    filePath        varchar(2048),
    sizeBytes       bigint,
    errorMessage    varchar(2048),
    domainIndexUsed integer,
    primary key (id)
);

create index download_requestedAt_idx on download (requestedAt desc);

create unique index download_active_md5_idx on download (md5) where status in ('PENDING', 'DOWNLOADING');
