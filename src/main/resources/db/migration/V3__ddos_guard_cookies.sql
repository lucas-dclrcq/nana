create sequence ddosGuardCookies_SEQ start with 1 increment by 50;

create table ddosGuardCookies
(
    id           bigint        not null,
    cookieHeader varchar(8192) not null,
    updatedAt    timestamp(6) with time zone not null,
    primary key (id)
);
