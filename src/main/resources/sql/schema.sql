-- auto-generated definition
create table if not exists choimory_io_member.member
(
    id          varchar(255) not null
    constraint member_pk
    primary key,
    email       varchar(255) not null,
    nickname    varchar(255) not null,
    password    varchar(255) not null,
    introduce   text,
    created_at  timestamp    not null,
    modified_at timestamp,
    deleted_at  timestamp
    );

create unique index if not exists  member_email_uindex
    on choimory_io_member.member (email);

create unique index if not exists  member_nickname_uindex
    on choimory_io_member.member (nickname);

